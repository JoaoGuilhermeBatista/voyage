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

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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

    private static final int MAX_LODGING_RESULTS_TO_GEOCODE = 10;

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
        this.apiKey = apiKey;
        this.restClient = restClient;
        this.destinationRepo = destinationRepo;
        this.poiRepo = poiRepo;
        this.geocodingService = geocodingService;
        this.llmService = llmService;
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

        List<String> candidates = discoveryCache.computeIfAbsent(destinationId,
            id -> discoverRealNeighborhoods(centerLat, centerLng));

        if (candidates.isEmpty()) {
            recommendationCache.put(cacheKey, List.of());
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

        recommendationCache.put(cacheKey, result);
        return new NeighborhoodSuggestionsDTO(true, result);
    }

    /**
     * STUB — a implementar por um próximo agente após revisão humana.
     *
     * Contrato:
     * <ul>
     *   <li>Chama a Google Places Nearby Search SEM filtro de preço (o
     *       filtro minprice/maxprice não funciona de forma confiável para
     *       lodging — o Google quase nunca preenche price_level para esse
     *       tipo — por isso foi removido):
     *       {@code https://maps.googleapis.com/maps/api/place/nearbysearch/json?location={lat},{lng}&radius=6000&type=lodging&key={apiKey}}.</li>
     *   <li>Para os primeiros {@value #MAX_LODGING_RESULTS_TO_GEOCODE}
     *       resultados ({@code results[0..9]}, cap por custo — não
     *       processar mais que isso), pega
     *       {@code geometry.location.{lat,lng}} de cada um e chama
     *       {@link GeocodingService#reverseGeocodeToNeighborhood(double, double)}.</li>
     *   <li>Agrega os nomes de bairro retornados (ignora
     *       {@code Optional.empty()}), deduplicando e ordenando por
     *       frequência (mais frequente primeiro) — sem limite adicional
     *       aqui, o corte final de quantos mostrar acontece depois, na
     *       recomendação por LLM.</li>
     *   <li>Se a Nearby Search der erro/status diferente de "OK"/
     *       "ZERO_RESULTS", ou erro de rede/parsing: loga warning, retorna
     *       lista vazia — NUNCA lança exceção.</li>
     *   <li>Se "ZERO_RESULTS" ou nenhum resultado render um bairro
     *       identificável: retorna lista vazia (comportamento esperado para
     *       cidades sem esse tipo de dado, ex: Paris às vezes).</li>
     * </ul>
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
}
