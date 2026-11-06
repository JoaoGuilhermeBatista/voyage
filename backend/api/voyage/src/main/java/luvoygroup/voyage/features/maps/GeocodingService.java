package luvoygroup.voyage.features.maps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import luvoygroup.voyage.features.maps.dto.LatLng;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Proxy do backend para a Google Geocoding API — converte um endereço
 * textual (ex.: "Bela Vista, São Paulo, Brasil") em coordenadas lat/lng,
 * usado para priorizar POIs próximos ao {@code stayArea} informado no
 * request de geração de roteiro. Sem api key configurada, os métodos
 * retornam {@code Optional.empty()} sem chamar a API; erro de rede/parsing
 * também vira {@code Optional.empty()} (nunca lança exceção).
 */
@Service
@Slf4j
public class GeocodingService {

    private static final String GEOCODE_URL_TEMPLATE =
        "https://maps.googleapis.com/maps/api/geocode/json?address=%s&key={apiKey}";

    private static final String REVERSE_GEOCODE_URL_TEMPLATE =
        "https://maps.googleapis.com/maps/api/geocode/json?latlng={lat},{lng}&key={apiKey}";

    private static final java.util.List<String> NEIGHBORHOOD_TYPE_PRIORITY =
        java.util.List.of("sublocality_level_1", "sublocality", "neighborhood");

    private final RestClient restClient;
    private final String apiKey;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public GeocodingService(@Value("${google.maps.api-key:not-configured}") String apiKey) {
        this(apiKey, RestClient.create());
    }

    /**
     * Construtor package-private, usado pelos testes para injetar um
     * {@link RestClient} mockado sem precisar subir o contexto Spring.
     */
    GeocodingService(String apiKey, RestClient restClient) {
        this.apiKey = apiKey;
        this.restClient = restClient;
    }

    public Optional<LatLng> geocode(String query) {
        if (!isConfigured()) {
            return Optional.empty();
        }

        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String urlTemplate = String.format(GEOCODE_URL_TEMPLATE, encodedQuery);

            String rawJson = restClient.get()
                .uri(urlTemplate, apiKey)
                .retrieve()
                .body(String.class);

            JsonNode root = objectMapper.readTree(rawJson);
            String status = root.path("status").asText();
            if (!"OK".equals(status)) {
                log.warn("Google Geocoding API retornou status inesperado: {}", status);
                return Optional.empty();
            }

            JsonNode firstResult = root.path("results").path(0);
            if (firstResult.isMissingNode()) {
                log.warn("Google Geocoding API retornou status OK sem resultados");
                return Optional.empty();
            }

            JsonNode location = firstResult.path("geometry").path("location");
            double lat = location.path("lat").asDouble();
            double lng = location.path("lng").asDouble();

            return Optional.of(new LatLng(lat, lng));
        } catch (Exception e) {
            log.warn("Erro ao geocodificar endereço via Google Geocoding API", e);
            return Optional.empty();
        }
    }

    /**
     * Geocodificação reversa (lat/lng → nome de bairro), usada por
     * {@code PlacesService#discoverRealNeighborhoods} porque o campo
     * {@code vicinity} da Google Places Nearby Search não é confiável pra
     * isso. Prioriza o primeiro {@code address_components[].types[]} que
     * bater com sublocality_level_1 > sublocality > neighborhood, nessa
     * ordem (não necessariamente a ordem em que aparecem no JSON).
     */
    public Optional<String> reverseGeocodeToNeighborhood(double lat, double lng) {
        if (!isConfigured()) {
            return Optional.empty();
        }

        try {
            String rawJson = restClient.get()
                .uri(REVERSE_GEOCODE_URL_TEMPLATE, lat, lng, apiKey)
                .retrieve()
                .body(String.class);

            JsonNode root = objectMapper.readTree(rawJson);
            String status = root.path("status").asText();
            if (!"OK".equals(status)) {
                log.warn("Google Geocoding API (reversa) retornou status inesperado: {}", status);
                return Optional.empty();
            }

            JsonNode firstResult = root.path("results").path(0);
            if (firstResult.isMissingNode()) {
                log.warn("Google Geocoding API (reversa) retornou status OK sem resultados");
                return Optional.empty();
            }

            JsonNode addressComponents = firstResult.path("address_components");
            String bestLongName = null;
            int bestPriority = Integer.MAX_VALUE;

            for (JsonNode component : addressComponents) {
                JsonNode typesNode = component.path("types");
                for (JsonNode typeNode : typesNode) {
                    int priority = NEIGHBORHOOD_TYPE_PRIORITY.indexOf(typeNode.asText());
                    if (priority >= 0 && priority < bestPriority) {
                        bestPriority = priority;
                        bestLongName = component.path("long_name").asText();
                    }
                }
            }

            return Optional.ofNullable(bestLongName);
        } catch (Exception e) {
            log.warn("Erro ao geocodificar reversamente coordenadas via Google Geocoding API", e);
            return Optional.empty();
        }
    }

    private boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank() && !"not-configured".equals(apiKey);
    }
}
