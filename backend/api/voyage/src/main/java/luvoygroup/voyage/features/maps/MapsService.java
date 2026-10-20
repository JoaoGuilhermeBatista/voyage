package luvoygroup.voyage.features.maps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import luvoygroup.voyage.domain.Destination;
import luvoygroup.voyage.exception.ResourceNotFoundException;
import luvoygroup.voyage.features.maps.dto.DirectionsResponseDTO;
import luvoygroup.voyage.features.maps.dto.EmbedUrlResponseDTO;
import luvoygroup.voyage.features.maps.dto.LatLng;
import luvoygroup.voyage.repository.DestinationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Optional;
import java.util.UUID;

/**
 * Proxy do backend para o Google Maps Platform (Directions + Embed).
 *
 * <ul>
 *   <li>api-key em branco/"not-configured" → configured=false, DTO com os
 *       demais campos nulos, NENHUMA chamada HTTP externa.</li>
 *   <li>api-key presente → getDirections chama a Directions API real e
 *       mapeia a resposta; getEmbedUrl apenas monta uma URL com a key
 *       (sem chamada HTTP).</li>
 * </ul>
 */
@Service
@Slf4j
public class MapsService {

    private static final String DIRECTIONS_URL =
        "https://maps.googleapis.com/maps/api/directions/json?origin={originLat},{originLng}&destination={destLat},{destLng}&key={apiKey}";

    private final RestClient restClient;
    private final String apiKey;
    private final GeocodingService geocodingService;
    private final DestinationRepository destinationRepo;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public MapsService(
        @Value("${google.maps.api-key:not-configured}") String apiKey,
        GeocodingService geocodingService,
        DestinationRepository destinationRepo
    ) {
        this(apiKey, RestClient.create(), geocodingService, destinationRepo);
    }

    /**
     * Construtor package-private, usado pelos testes para injetar um
     * {@link RestClient} mockado sem precisar subir o contexto Spring.
     */
    MapsService(
        String apiKey,
        RestClient restClient,
        GeocodingService geocodingService,
        DestinationRepository destinationRepo
    ) {
        this.apiKey = apiKey;
        this.restClient = restClient;
        this.geocodingService = geocodingService;
        this.destinationRepo = destinationRepo;
    }

    public DirectionsResponseDTO getDirections(double originLat, double originLng, double destLat, double destLng) {
        if (!isConfigured()) {
            return new DirectionsResponseDTO(false, null, null, null, null);
        }

        String rawJson = restClient.get()
            .uri(DIRECTIONS_URL, originLat, originLng, destLat, destLng, apiKey)
            .retrieve()
            .body(String.class);

        try {
            JsonNode root = objectMapper.readTree(rawJson);
            JsonNode leg = root.path("routes").path(0).path("legs").path(0);
            JsonNode distance = leg.path("distance");
            JsonNode duration = leg.path("duration");

            Double distanceMeters = (double) distance.path("value").asInt();
            Integer durationSeconds = duration.path("value").asInt();
            String distanceText = distance.path("text").asText();
            String durationText = duration.path("text").asText();

            return new DirectionsResponseDTO(true, distanceMeters, durationSeconds, distanceText, durationText);
        } catch (Exception e) {
            throw new IllegalStateException("Resposta inválida da Google Directions API", e);
        }
    }

    /**
     * Distância/duração de deslocamento entre as CIDADES de dois destinos
     * (ex.: trechos consecutivos de uma viagem multi-destino) — diferente
     * de {@link #getDirections}, que já recebe lat/lng prontos (POIs, que
     * sempre têm coordenada própria). {@code Destination} não guarda
     * coordenada própria, então geocodifica "cidade, país" de cada um sob
     * demanda (cacheado por par de destino, 24h, mesma anotação do
     * câmbio/vizinhança). {@code configured=true} com os demais campos
     * nulos quando algum dos dois destinos não geocodifica (ex.: Directions
     * não tem rota terrestre entre eles) — o app esconde a UI graciosamente,
     * nunca lança erro pro usuário por essa causa.
     */
    @Cacheable("directions-between-destinations")
    public DirectionsResponseDTO getDirectionsBetweenDestinations(UUID originDestinationId, UUID destDestinationId) {
        if (!isConfigured()) {
            return new DirectionsResponseDTO(false, null, null, null, null);
        }

        Destination origin = destinationRepo.findById(originDestinationId)
            .orElseThrow(() -> new ResourceNotFoundException("Destino não encontrado"));
        Destination destination = destinationRepo.findById(destDestinationId)
            .orElseThrow(() -> new ResourceNotFoundException("Destino não encontrado"));

        Optional<LatLng> originLatLng = geocodingService.geocode(origin.getCity() + ", " + origin.getCountry());
        Optional<LatLng> destLatLng = geocodingService.geocode(destination.getCity() + ", " + destination.getCountry());
        if (originLatLng.isEmpty() || destLatLng.isEmpty()) {
            return new DirectionsResponseDTO(true, null, null, null, null);
        }

        try {
            return getDirections(
                originLatLng.get().lat(), originLatLng.get().lng(),
                destLatLng.get().lat(), destLatLng.get().lng());
        } catch (Exception e) {
            log.warn("Erro ao buscar rota entre {} e {}", origin.getSlug(), destination.getSlug(), e);
            return new DirectionsResponseDTO(true, null, null, null, null);
        }
    }

    public EmbedUrlResponseDTO getEmbedUrl(double lat, double lng) {
        if (!isConfigured()) {
            return new EmbedUrlResponseDTO(false, null);
        }

        String embedUrl = "https://www.google.com/maps/embed/v1/place?key=" + apiKey + "&q=" + lat + "," + lng;
        return new EmbedUrlResponseDTO(true, embedUrl);
    }

    /**
     * Mesma lógica de {@link #getEmbedUrl}, mas no modo "directions" da
     * Embed API — em vez de um pino num único ponto, mostra a rota
     * traçada entre origem e destino. Também não faz chamada HTTP (só
     * monta a URL com a key para o app renderizar num iframe/WebView).
     */
    public EmbedUrlResponseDTO getDirectionsEmbedUrl(double originLat, double originLng, double destLat, double destLng) {
        if (!isConfigured()) {
            return new EmbedUrlResponseDTO(false, null);
        }

        String embedUrl = "https://www.google.com/maps/embed/v1/directions?key=" + apiKey
            + "&origin=" + originLat + "," + originLng
            + "&destination=" + destLat + "," + destLng;
        return new EmbedUrlResponseDTO(true, embedUrl);
    }

    private boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank() && !"not-configured".equals(apiKey);
    }
}
