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
 * textual (ex.: "Bela Vista, São Paulo, Brasil") em coordenadas
 * lat/lng, usado para priorizar POIs próximos ao {@code stayArea}
 * informado no request de geração de roteiro.
 *
 * A checagem de {@code isConfigured()} é real e feita inline em
 * {@link #geocode(String)} (mesmo critério de
 * {@code MapsService.isConfigured()}: null/blank/"not-configured") — sem
 * api key, o método retorna {@code Optional.empty()} imediatamente, SEM
 * chamar o RestClient. O restante — a chamada HTTP à Geocoding API e o
 * parsing da resposta — é um STUB COMPLETO, a implementar por um próximo
 * agente após revisão humana. Contrato do trecho stub:
 * <ul>
 *   <li>Chama
 *       {@code https://maps.googleapis.com/maps/api/geocode/json?address={query}&key={apiKey}}
 *       (query deve ser URL-encoded corretamente).</li>
 *   <li>Se o status da resposta for "OK" e {@code results} não vazio →
 *       pega {@code results[0].geometry.location.lat} e
 *       {@code results[0].geometry.location.lng}, retorna
 *       {@code Optional.of(new LatLng(lat, lng))}.</li>
 *   <li>Qualquer outro status, resultado vazio, erro de rede ou parsing →
 *       loga warning, retorna {@code Optional.empty()} — NUNCA lança
 *       exceção.</li>
 * </ul>
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
     * Geocodificação REVERSA (lat/lng → nome de bairro), usada por
     * {@code PlacesService#discoverRealNeighborhoods} para extrair um nome
     * de bairro real a partir das coordenadas de cada resultado da Google
     * Places Nearby Search (o campo {@code vicinity} da Nearby Search NÃO é
     * confiável para isso — ver contexto na tarefa que introduziu este
     * método).
     *
     * A checagem de {@code isConfigured()} é real e feita inline logo no
     * início do método (mesmo critério de {@link #geocode(String)}:
     * null/blank/"not-configured") — sem api key, retorna
     * {@code Optional.empty()} imediatamente, SEM chamar o RestClient. O
     * restante — a chamada HTTP à Geocoding API e o parsing da resposta — é
     * um STUB COMPLETO, a implementar por um próximo agente após revisão
     * humana. Contrato do trecho stub:
     * <ul>
     *   <li>Chama
     *       {@code https://maps.googleapis.com/maps/api/geocode/json?latlng={lat},{lng}&key={apiKey}}.</li>
     *   <li>Se o status da resposta não for "OK", ou {@code results} vier
     *       vazio, ou der erro de rede/parsing → loga warning, retorna
     *       {@code Optional.empty()} — NUNCA lança exceção.</li>
     *   <li>No primeiro resultado ({@code results[0]}), procura em
     *       {@code address_components[]} um componente cujo {@code types[]}
     *       contenha, nesta ordem de prioridade: {@code
     *       "sublocality_level_1"}, depois {@code "sublocality"}, depois
     *       {@code "neighborhood"} — retorna o {@code long_name} do
     *       PRIMEIRO componente encontrado nessa ordem de prioridade (não é
     *       "o primeiro componente da lista que bate com qualquer um dos
     *       três": é sempre preferir sublocality_level_1 sobre sublocality
     *       sobre neighborhood, mesmo que apareçam fora de ordem no JSON).</li>
     *   <li>Se {@code results[0]} não tiver nenhum componente com um desses
     *       três types (ex.: Paris às vezes só tem arrondissement/locality),
     *       retorna {@code Optional.empty()} — comportamento esperado, não
     *       é erro.</li>
     * </ul>
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
