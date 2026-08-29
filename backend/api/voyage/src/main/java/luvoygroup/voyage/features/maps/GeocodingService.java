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

    private boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank() && !"not-configured".equals(apiKey);
    }
}
