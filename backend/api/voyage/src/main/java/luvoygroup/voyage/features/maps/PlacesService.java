package luvoygroup.voyage.features.maps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import luvoygroup.voyage.domain.Destination;
import luvoygroup.voyage.domain.Poi;
import luvoygroup.voyage.domain.enums.BudgetTier;
import luvoygroup.voyage.exception.ResourceNotFoundException;
import luvoygroup.voyage.features.maps.dto.NeighborhoodSuggestionsDTO;
import luvoygroup.voyage.repository.DestinationRepository;
import luvoygroup.voyage.repository.PoiRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Proxy do backend para a Google Places API (Nearby Search), usado para
 * sugerir bairros para hospedagem com base no centróide dos POIs ativos de
 * um destino.
 *
 * <ul>
 *   <li>api-key em branco/"not-configured" → configured=false, lista vazia,
 *       NENHUMA chamada HTTP externa.</li>
 *   <li>destino sem POIs com coordenadas → configured=true, lista vazia
 *       (não há como calcular um centro), sem chamar a Places API.</li>
 *   <li>api-key presente e há POIs com coordenadas → calcula o centróide e
 *       delega para {@link #fetchFromPlaces(double, double, BudgetTier)},
 *       cacheando o resultado em memória por destino+budgetTier.</li>
 * </ul>
 */
@Service
@Slf4j
public class PlacesService {

    private final RestClient restClient;
    private final String apiKey;
    private final DestinationRepository destinationRepo;
    private final PoiRepository poiRepo;

    private static final String NEARBY_SEARCH_URL_TEMPLATE =
        "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location={lat},{lng}&radius=6000&type=lodging&minprice=%d&maxprice=%d&key={apiKey}";

    private static final int MAX_NEIGHBORHOODS = 6;

    private final Map<String, List<String>> cache = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public PlacesService(
        @Value("${google.maps.api-key:not-configured}") String apiKey,
        DestinationRepository destinationRepo,
        PoiRepository poiRepo
    ) {
        this(apiKey, RestClient.create(), destinationRepo, poiRepo);
    }

    /**
     * Construtor package-private, usado pelos testes para injetar um
     * {@link RestClient} mockado sem precisar subir o contexto Spring.
     */
    PlacesService(String apiKey, RestClient restClient, DestinationRepository destinationRepo, PoiRepository poiRepo) {
        this.apiKey = apiKey;
        this.restClient = restClient;
        this.destinationRepo = destinationRepo;
        this.poiRepo = poiRepo;
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
        List<String> cached = cache.get(cacheKey);
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

        List<String> result = fetchFromPlaces(centerLat, centerLng, budgetTier);
        cache.put(cacheKey, result);
        return new NeighborhoodSuggestionsDTO(true, result);
    }

    /**
     * STUB — a implementar por um próximo agente após revisão humana.
     *
     * Contrato:
     * <ul>
     *   <li>Chama a Google Places Nearby Search:
     *       {@code https://maps.googleapis.com/maps/api/place/nearbysearch/json?location={lat},{lng}&radius=6000&type=lodging&minprice={min}&maxprice={max}&key={apiKey}}.</li>
     *   <li>Mapeamento de {@code budgetTier} para price_level do Google:
     *       ECONOMY → minprice=0, maxprice=1; MODERATE → minprice=2,
     *       maxprice=2; LUXURY → minprice=3, maxprice=4.</li>
     *   <li>Parseia {@code results[].vicinity} de cada resultado (campo de
     *       endereço curto que a Nearby Search já retorna, sem chamada
     *       extra de geocoding).</li>
     *   <li>Para cada vicinity, faz split por vírgula e pega o ÚLTIMO
     *       segmento não-vazio (trim) como candidato a nome de bairro
     *       (heurística: "Rua X, Bairro Y" → "Bairro Y"; se não tiver
     *       vírgula, usa a string inteira trimada).</li>
     *   <li>Agrega por frequência (quantas vezes cada nome de bairro
     *       aparece nos resultados), ordena por frequência decrescente,
     *       retorna os top 6 nomes ÚNICOS.</li>
     *   <li>NUNCA retorna nome de hotel, place_id, rating ou qualquer dado
     *       do estabelecimento em si — só os nomes de bairro agregados.</li>
     *   <li>Se o status da resposta não for "OK" nem "ZERO_RESULTS", ou se
     *       der qualquer erro de parsing/rede: loga um warning e retorna
     *       lista vazia — NUNCA lança exceção (mesmo espírito de graceful
     *       degradation do resto do projeto, ex:
     *       AdminService.enrichExistingPoisWithMustSeeAndLandmark).</li>
     * </ul>
     */
    private List<String> fetchFromPlaces(double lat, double lng, BudgetTier budgetTier) {
        int minPrice;
        int maxPrice;
        switch (budgetTier) {
            case ECONOMY -> {
                minPrice = 0;
                maxPrice = 1;
            }
            case LUXURY -> {
                minPrice = 3;
                maxPrice = 4;
            }
            case MODERATE -> {
                minPrice = 2;
                maxPrice = 2;
            }
            default -> {
                minPrice = 2;
                maxPrice = 2;
            }
        }

        String urlTemplate = String.format(NEARBY_SEARCH_URL_TEMPLATE, minPrice, maxPrice);

        try {
            String rawJson = restClient.get()
                .uri(urlTemplate, lat, lng, apiKey)
                .retrieve()
                .body(String.class);

            JsonNode root = objectMapper.readTree(rawJson);
            String status = root.path("status").asText();
            if (!"OK".equals(status) && !"ZERO_RESULTS".equals(status)) {
                log.warn("Google Places Nearby Search retornou status inesperado: {}", status);
                return List.of();
            }

            Map<String, Integer> frequencyByNeighborhood = new LinkedHashMap<>();
            for (JsonNode result : root.path("results")) {
                String vicinity = result.path("vicinity").asText(null);
                if (vicinity == null || vicinity.isBlank()) {
                    continue;
                }

                String[] segments = vicinity.split(",");
                String neighborhood = null;
                for (int i = segments.length - 1; i >= 0; i--) {
                    String candidate = segments[i].trim();
                    if (!candidate.isEmpty()) {
                        neighborhood = candidate;
                        break;
                    }
                }
                if (neighborhood == null) {
                    continue;
                }

                frequencyByNeighborhood.merge(neighborhood, 1, Integer::sum);
            }

            return frequencyByNeighborhood.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .map(Map.Entry::getKey)
                .distinct()
                .limit(MAX_NEIGHBORHOODS)
                .toList();
        } catch (Exception e) {
            log.warn("Erro ao buscar bairros via Google Places Nearby Search", e);
            return List.of();
        }
    }

    private boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank() && !"not-configured".equals(apiKey);
    }
}
