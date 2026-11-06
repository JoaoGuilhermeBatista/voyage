package luvoygroup.voyage.features.maps;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import luvoygroup.voyage.domain.Destination;
import luvoygroup.voyage.domain.Poi;
import luvoygroup.voyage.domain.enums.BudgetTier;
import luvoygroup.voyage.exception.ResourceNotFoundException;
import luvoygroup.voyage.features.admin.LlmService;
import luvoygroup.voyage.features.maps.dto.NeighborhoodRecommendation;
import luvoygroup.voyage.features.maps.dto.NeighborhoodSuggestionsDTO;
import luvoygroup.voyage.repository.DestinationRepository;
import luvoygroup.voyage.repository.PoiRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Proxy do backend para a Google Places API (Nearby Search) + Google
 * Geocoding API (reversa) + LLM, usado para sugerir bairros REAIS para
 * hospedagem com base no centróide dos POIs ativos de um destino, cada um
 * acompanhado de uma frase de vantagem adequada à faixa de budget
 * consultada.
 *
 * <ul>
 *   <li>api-key em branco/"not-configured" → configured=false, lista vazia,
 *       NENHUMA chamada HTTP externa.</li>
 *   <li>destino sem POIs com coordenadas → configured=true, lista vazia
 *       (não há como calcular um centro), sem chamar a Places API.</li>
 *   <li>api-key presente e há POIs com coordenadas → calcula o centróide,
 *       descobre bairros reais via {@link #discoverRealNeighborhoods(double, double)}
 *       (cacheado por destino, independente de budgetTier — ver
 *       {@link #discoveryCache}) e pede ao {@link LlmService} para
 *       recomendar um subconjunto adequado ao budgetTier informado, com
 *       frase de vantagem (cacheado por destino+budgetTier — ver
 *       {@link #recommendationCache}).</li>
 * </ul>
 */
@Service
@Slf4j
public class PlacesService {

    private final RestClient restClient;
    private final String apiKey;
    private final DestinationRepository destinationRepo;
    private final PoiRepository poiRepo;
    private final GeocodingService geocodingService;
    private final LlmService llmService;

    private static final String NEARBY_SEARCH_URL_TEMPLATE =
        "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location={lat},{lng}&radius=6000&type=lodging&key={apiKey}";

    private static final String FIND_PLACE_URL_TEMPLATE =
        "https://maps.googleapis.com/maps/api/place/findplacefromtext/json?input={input}&inputtype=textquery&fields=place_id&key={apiKey}";

    private static final String PLACE_DETAILS_URL_TEMPLATE =
        "https://maps.googleapis.com/maps/api/place/details/json?place_id={placeId}&fields=photos&key={apiKey}";

    private static final String PLACE_PHOTO_URL_TEMPLATE =
        "https://maps.googleapis.com/maps/api/place/photo?maxwidth=1200&photo_reference=%s&key=%s";

    private static final int MAX_LODGING_RESULTS_TO_GEOCODE = 10;

    /**
     * Resolve a URL final (sem api-key) de uma foto do Google Places,
     * seguindo manualmente o redirect 302 que o endpoint Place Photo
     * devolve em vez de deixar o cliente HTTP seguir automaticamente — só
     * assim dá pra capturar a URL de destino (lh3.googleusercontent.com)
     * sem baixar a imagem inteira. Trocável nos testes (ver construtor de
     * 7 argumentos); a implementação real usa java.net.http.HttpClient
     * puro porque RestClient não expõe esse controle de redirect.
     */
    private final Function<String, Optional<String>> redirectResolver;

    /**
     * Cache de descoberta de bairros reais, por destinationId apenas (SEM
     * budgetTier) — a descoberta (Nearby Search + reverse geocode) não
     * depende de budget, então cachear por destinationId+budgetTier
     * triplicaria o custo para os 3 tiers do mesmo destino.
     */
    private final Map<UUID, List<String>> discoveryCache = new ConcurrentHashMap<>();

    /**
     * Cache de recomendações finais (após passar pelo LLM), por
     * destinationId+budgetTier — a recomendação em si é específica de
     * budget.
     */
    private final Map<String, List<NeighborhoodRecommendation>> recommendationCache = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public PlacesService(
        @Value("${google.maps.api-key:not-configured}") String apiKey,
        DestinationRepository destinationRepo,
        PoiRepository poiRepo,
        GeocodingService geocodingService,
        LlmService llmService
    ) {
        this(apiKey, RestClient.create(), destinationRepo, poiRepo, geocodingService, llmService);
    }

    /**
     * Construtor package-private, usado pelos testes para injetar um
     * {@link RestClient} mockado sem precisar subir o contexto Spring.
     */
    PlacesService(
        String apiKey,
        RestClient restClient,
        DestinationRepository destinationRepo,
        PoiRepository poiRepo,
        GeocodingService geocodingService,
        LlmService llmService
    ) {
        this(apiKey, restClient, destinationRepo, poiRepo, geocodingService, llmService,
            PlacesService::resolvePhotoRedirectReal);
    }

    /**
     * Construtor package-private com {@link #redirectResolver} explícito —
     * usado pelos testes de {@link #fetchPhotoUrl} para mockar a resolução
     * do redirect sem fazer chamada HTTP de verdade.
     */
    PlacesService(
        String apiKey,
        RestClient restClient,
        DestinationRepository destinationRepo,
        PoiRepository poiRepo,
        GeocodingService geocodingService,
        LlmService llmService,
        Function<String, Optional<String>> redirectResolver
    ) {
        this.apiKey = apiKey;
        this.restClient = restClient;
        this.destinationRepo = destinationRepo;
        this.poiRepo = poiRepo;
        this.geocodingService = geocodingService;
        this.llmService = llmService;
        this.redirectResolver = redirectResolver;
    }

    public NeighborhoodSuggestionsDTO suggestNeighborhoods(UUID destinationId, BudgetTier budgetTier) {
        Destination destination = destinationRepo.findById(destinationId)
            .orElseThrow(() -> new ResourceNotFoundException("Destino não encontrado"));
        if (!destination.isActive()) {
            throw new ResourceNotFoundException("Destino não encontrado");
        }

        if (!isConfigured()) {
            return new NeighborhoodSuggestionsDTO(false, List.of());
        }

        List<Poi> poisWithCoords = poiRepo.findAllByDestinationIdAndActiveTrue(destinationId).stream()
            .filter(p -> p.getLatitude() != null && p.getLongitude() != null)
            .toList();

        if (poisWithCoords.isEmpty()) {
            return new NeighborhoodSuggestionsDTO(true, List.of());
        }

        String cacheKey = destinationId + ":" + budgetTier;
        List<NeighborhoodRecommendation> cached = recommendationCache.get(cacheKey);
        if (cached != null) {
            return new NeighborhoodSuggestionsDTO(true, cached);
        }

        double centerLat = poisWithCoords.stream()
            .mapToDouble(p -> p.getLatitude().doubleValue())
            .average()
            .orElseThrow();
        double centerLng = poisWithCoords.stream()
            .mapToDouble(p -> p.getLongitude().doubleValue())
            .average()
            .orElseThrow();

        // Não usamos computeIfAbsent: uma lista vazia pode vir de uma falha
        // transitória (rede, quota) indistinguível de "sem bairro
        // identificável de verdade" — só memoizamos resultado NÃO-vazio,
        // pra não travar a feature pro resto da vida do processo por causa
        // de um erro pontual.
        List<String> candidates = discoveryCache.get(destinationId);
        if (candidates == null) {
            candidates = discoverRealNeighborhoods(centerLat, centerLng);
            if (!candidates.isEmpty()) {
                discoveryCache.put(destinationId, candidates);
            }
        }

        if (candidates.isEmpty()) {
            return new NeighborhoodSuggestionsDTO(true, List.of());
        }

        List<NeighborhoodRecommendation> result;
        try {
            result = llmService.recommendNeighborhoods(
                destination.getCity(), destination.getCountry(), candidates, budgetTier);
        } catch (Exception e) {
            log.warn("Falha ao gerar recomendações de bairro via LLM para destino {}", destinationId, e);
            result = List.of();
        }

        // Mesmo motivo do discoveryCache acima: só memoiza resultado real.
        if (!result.isEmpty()) {
            recommendationCache.put(cacheKey, result);
        }
        return new NeighborhoodSuggestionsDTO(true, result);
    }

    /**
     * Busca lodging via Nearby Search (sem filtro de preço — o Google quase
     * nunca preenche price_level pra esse tipo) nos primeiros
     * {@value #MAX_LODGING_RESULTS_TO_GEOCODE} resultados, reverse-geocodifica
     * cada um e retorna os nomes de bairro por frequência (mais frequente
     * primeiro). Nunca lança exceção — erro ou zero resultado vira lista vazia.
     */
    private List<String> discoverRealNeighborhoods(double lat, double lng) {
        try {
            String rawJson = restClient.get()
                .uri(NEARBY_SEARCH_URL_TEMPLATE, lat, lng, apiKey)
                .retrieve()
                .body(String.class);

            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(rawJson);
            String status = root.path("status").asText();
            if (!"OK".equals(status) && !"ZERO_RESULTS".equals(status)) {
                log.warn("Google Places Nearby Search retornou status inesperado: {}", status);
                return List.of();
            }

            com.fasterxml.jackson.databind.JsonNode results = root.path("results");
            Map<String, Integer> frequency = new java.util.LinkedHashMap<>();

            int limit = Math.min(results.size(), MAX_LODGING_RESULTS_TO_GEOCODE);
            for (int i = 0; i < limit; i++) {
                com.fasterxml.jackson.databind.JsonNode result = results.path(i);
                com.fasterxml.jackson.databind.JsonNode location = result.path("geometry").path("location");
                double resultLat = location.path("lat").asDouble();
                double resultLng = location.path("lng").asDouble();

                geocodingService.reverseGeocodeToNeighborhood(resultLat, resultLng)
                    .ifPresent(neighborhood -> frequency.merge(neighborhood, 1, Integer::sum));
            }

            return frequency.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .toList();
        } catch (Exception e) {
            log.warn("Erro ao descobrir bairros reais via Google Places Nearby Search", e);
            return List.of();
        }
    }

    private boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank() && !"not-configured".equals(apiKey);
    }

    /**
     * Busca 1 foto real no Google Places para um POI, a partir do nome e da
     * cidade do destino — usado por
     * {@code AdminService#enrichExistingPoisWithImages} para popular
     * {@code Poi.images} retroativamente. Fluxo: Find Place From Text (nome
     * + cidade → place_id) → Place Details (place_id → photo_reference) →
     * Place Photo (segue o redirect 302 e devolve a URL final do CDN do
     * Google, sem api-key embutida). Nunca lança exceção — qualquer falha
     * (api-key ausente, lugar não encontrado, sem fotos, erro de rede) vira
     * {@code Optional.empty()}, logado como warning.
     */
    public Optional<String> fetchPhotoUrl(String title, String city) {
        if (!isConfigured()) {
            return Optional.empty();
        }

        try {
            String findPlaceJson = restClient.get()
                .uri(FIND_PLACE_URL_TEMPLATE, title + ", " + city, apiKey)
                .retrieve()
                .body(String.class);

            com.fasterxml.jackson.databind.JsonNode findPlaceRoot = objectMapper.readTree(findPlaceJson);
            if (!"OK".equals(findPlaceRoot.path("status").asText())) {
                return Optional.empty();
            }
            com.fasterxml.jackson.databind.JsonNode candidates = findPlaceRoot.path("candidates");
            if (!candidates.isArray() || candidates.isEmpty()) {
                return Optional.empty();
            }
            String placeId = candidates.get(0).path("place_id").asText(null);
            if (placeId == null) {
                return Optional.empty();
            }

            String detailsJson = restClient.get()
                .uri(PLACE_DETAILS_URL_TEMPLATE, placeId, apiKey)
                .retrieve()
                .body(String.class);

            com.fasterxml.jackson.databind.JsonNode detailsRoot = objectMapper.readTree(detailsJson);
            if (!"OK".equals(detailsRoot.path("status").asText())) {
                return Optional.empty();
            }
            com.fasterxml.jackson.databind.JsonNode photos = detailsRoot.path("result").path("photos");
            if (!photos.isArray() || photos.isEmpty()) {
                return Optional.empty();
            }
            String photoReference = photos.get(0).path("photo_reference").asText(null);
            if (photoReference == null) {
                return Optional.empty();
            }

            String photoUrl = PLACE_PHOTO_URL_TEMPLATE.formatted(
                URLEncoder.encode(photoReference, StandardCharsets.UTF_8), apiKey);
            return redirectResolver.apply(photoUrl);
        } catch (Exception e) {
            log.warn("Erro ao buscar foto no Google Places para '{}' ({})", title, city, e);
            return Optional.empty();
        }
    }

    private static Optional<String> resolvePhotoRedirectReal(String url) {
        try {
            HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() == 301 || response.statusCode() == 302) {
                return response.headers().firstValue("Location");
            }
            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
