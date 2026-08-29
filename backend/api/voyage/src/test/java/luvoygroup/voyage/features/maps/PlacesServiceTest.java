package luvoygroup.voyage.features.maps;

import luvoygroup.voyage.domain.Destination;
import luvoygroup.voyage.domain.Poi;
import luvoygroup.voyage.domain.enums.BudgetTier;
import luvoygroup.voyage.domain.enums.PoiCategory;
import luvoygroup.voyage.exception.ResourceNotFoundException;
import luvoygroup.voyage.features.maps.dto.NeighborhoodSuggestionsDTO;
import luvoygroup.voyage.repository.DestinationRepository;
import luvoygroup.voyage.repository.PoiRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Testes unitários "standalone" de PlacesService — mesmo padrão de
 * MapsServiceTest (ver comentário no topo daquele arquivo): NÃO usa
 * @ExtendWith(MockitoExtension.class) de propósito (evitaria falso-positivo
 * de "unnecessary stubbing" nos cenários que dependem do stub
 * fetchFromPlaces, hoje inerte), e PlacesService é instanciado diretamente
 * via seu construtor package-private (String apiKey, RestClient restClient,
 * DestinationRepository, PoiRepository).
 *
 * IMPORTANTE (estado atual — RED por design nos testes marcados como tal):
 * PlacesService#fetchFromPlaces é um STUB que lança
 * UnsupportedOperationException (ver PlacesService.java) até o próximo
 * agente implementar a lógica real de chamada à Google Places Nearby
 * Search. Os testes que dependem desse método chamam suggestNeighborhoods
 * diretamente (sem assertThrows) e afirmam o comportamento DESEJADO — por
 * isso devem FALHAR agora e passar a verde quando fetchFromPlaces for
 * implementado.
 */
class PlacesServiceTest {

    private static final String FAKE_API_KEY = "fake-google-places-key-for-tests";

    // JSON de exemplo no shape real da resposta da Google Places Nearby
    // Search (https://maps.googleapis.com/maps/api/place/nearbysearch/json),
    // reduzido aos campos que a implementação precisa mapear. Repetimos
    // "Bela Vista" e "Consolação" para exercitar a agregação por frequência:
    // Bela Vista aparece 3x, Consolação 2x, os demais 1x cada.
    private static final String SAMPLE_NEARBY_SEARCH_JSON = """
        {
          "status": "OK",
          "results": [
            { "name": "Hotel A", "vicinity": "Av. Paulista, Bela Vista" },
            { "name": "Hotel B", "vicinity": "Rua Augusta, Consolação" },
            { "name": "Hotel C", "vicinity": "Alameda Santos, Bela Vista" },
            { "name": "Hotel D", "vicinity": "Rua da Consolação, Consolação" },
            { "name": "Hotel E", "vicinity": "Rua Oscar Freire, Jardins" },
            { "name": "Hotel F", "vicinity": "Rua Bela Cintra, Bela Vista" }
          ]
        }
        """;

    private static final String ZERO_RESULTS_JSON = """
        { "status": "ZERO_RESULTS", "results": [] }
        """;

    private DestinationRepository destinationRepo;
    private PoiRepository poiRepo;
    private RestClient mockRestClient;
    private UUID destinationId;
    private Destination destination;

    @BeforeEach
    void setUp() {
        destinationRepo = mock(DestinationRepository.class);
        poiRepo = mock(PoiRepository.class);
        mockRestClient = mock(RestClient.class);
        destinationId = UUID.randomUUID();
        destination = Destination.builder()
            .id(destinationId).name("São Paulo").city("São Paulo").country("Brasil")
            .slug("sao-paulo").tagline("Cidade que não para").active(true).build();
    }

    private List<Poi> poisWithCoords() {
        return List.of(
            poiAt(BigDecimal.valueOf(-23.5610), BigDecimal.valueOf(-46.6560)),
            poiAt(BigDecimal.valueOf(-23.5620), BigDecimal.valueOf(-46.6550))
        );
    }

    private Poi poiAt(BigDecimal lat, BigDecimal lng) {
        return Poi.builder()
            .id(UUID.randomUUID())
            .destination(destination)
            .title("POI-" + UUID.randomUUID().toString().substring(0, 6))
            .category(PoiCategory.MUSEUM)
            .budgetTier(BudgetTier.MODERATE)
            .active(true)
            .latitude(lat)
            .longitude(lng)
            .build();
    }

    // ─── (a) api-key não configurada → configured=false, sem chamada HTTP ─

    @Test
    void suggestNeighborhoods_shouldReturnNotConfigured_andNeverTouchRestClientOrPoiRepo_whenApiKeyIsBlank() {
        when(destinationRepo.findById(destinationId)).thenReturn(Optional.of(destination));
        PlacesService service = new PlacesService("not-configured", mockRestClient, destinationRepo, poiRepo);

        NeighborhoodSuggestionsDTO result = service.suggestNeighborhoods(destinationId, BudgetTier.MODERATE);

        assertFalse(result.configured());
        assertTrue(result.neighborhoods().isEmpty());
        verifyNoInteractions(mockRestClient);
        verifyNoInteractions(poiRepo);
    }

    // ─── (b) destino inexistente → ResourceNotFoundException ──────────────

    @Test
    void suggestNeighborhoods_shouldThrowResourceNotFound_whenDestinationDoesNotExist() {
        when(destinationRepo.findById(destinationId)).thenReturn(Optional.empty());
        PlacesService service = new PlacesService(FAKE_API_KEY, mockRestClient, destinationRepo, poiRepo);

        assertThrows(ResourceNotFoundException.class,
            () -> service.suggestNeighborhoods(destinationId, BudgetTier.MODERATE));
    }

    // ─── (c) destino sem nenhum POI com lat/lng → configured=true, lista vazia, sem chamar RestClient ─

    @Test
    void suggestNeighborhoods_shouldReturnEmptyButConfigured_whenNoPoiHasCoordinates() {
        when(destinationRepo.findById(destinationId)).thenReturn(Optional.of(destination));
        Poi poiWithoutCoords = Poi.builder()
            .id(UUID.randomUUID()).destination(destination).title("Sem coords")
            .category(PoiCategory.MUSEUM).budgetTier(BudgetTier.MODERATE).active(true)
            .build();
        when(poiRepo.findAllByDestinationIdAndActiveTrue(destinationId))
            .thenReturn(List.of(poiWithoutCoords));

        PlacesService service = new PlacesService(FAKE_API_KEY, mockRestClient, destinationRepo, poiRepo);

        NeighborhoodSuggestionsDTO result = service.suggestNeighborhoods(destinationId, BudgetTier.MODERATE);

        assertTrue(result.configured());
        assertTrue(result.neighborhoods().isEmpty());
        verifyNoInteractions(mockRestClient);
    }

    // ─── (d) fluxo completo — RED, depende do stub fetchFromPlaces ────────
    //
    // Contrato assumido: fetchFromPlaces chama a Nearby Search, parseia
    // vicinity de cada resultado, pega o último segmento após vírgula,
    // agrega por frequência e retorna os top 6 nomes únicos, mais frequente
    // primeiro. Com o JSON de exemplo acima: Bela Vista (3x), Consolação
    // (2x), Jardins (1x) — nessa ordem.

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void suggestNeighborhoods_shouldReturnAggregatedNeighborhoods_whenFullFlowSucceeds() {
        when(destinationRepo.findById(destinationId)).thenReturn(Optional.of(destination));
        when(poiRepo.findAllByDestinationIdAndActiveTrue(destinationId)).thenReturn(poisWithCoords());

        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        when(mockRestClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString(), any(Object[].class))).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenReturn(SAMPLE_NEARBY_SEARCH_JSON);

        PlacesService service = new PlacesService(FAKE_API_KEY, mockRestClient, destinationRepo, poiRepo);

        NeighborhoodSuggestionsDTO result = service.suggestNeighborhoods(destinationId, BudgetTier.MODERATE);

        assertTrue(result.configured());
        assertEquals(List.of("Bela Vista", "Consolação", "Jardins"), result.neighborhoods());
    }

    // ─── (e) cache — RED, depende do stub rodar com sucesso na 1ª chamada ──
    //
    // Nota: como fetchFromPlaces hoje lança UnsupportedOperationException,
    // este teste falha já na primeira chamada a suggestNeighborhoods (antes
    // mesmo de exercitar o cache em si) — ele documenta o comportamento
    // DESEJADO (2ª chamada com mesmo destinationId+budgetTier não deve
    // tocar o RestClient de novo) e deve virar verde assim que
    // fetchFromPlaces for implementado.

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void suggestNeighborhoods_shouldNotCallRestClientAgain_onSecondCallWithSameParams() {
        when(destinationRepo.findById(destinationId)).thenReturn(Optional.of(destination));
        when(poiRepo.findAllByDestinationIdAndActiveTrue(destinationId)).thenReturn(poisWithCoords());

        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        when(mockRestClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString(), any(Object[].class))).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenReturn(SAMPLE_NEARBY_SEARCH_JSON);

        PlacesService service = new PlacesService(FAKE_API_KEY, mockRestClient, destinationRepo, poiRepo);

        service.suggestNeighborhoods(destinationId, BudgetTier.MODERATE);
        service.suggestNeighborhoods(destinationId, BudgetTier.MODERATE);

        verify(mockRestClient, times(1)).get();
    }

    // ─── (f) erro/parsing/rede → lista vazia, nunca lança — RED ────────────

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void suggestNeighborhoods_shouldReturnEmptyList_whenNearbySearchStatusIsError() {
        when(destinationRepo.findById(destinationId)).thenReturn(Optional.of(destination));
        when(poiRepo.findAllByDestinationIdAndActiveTrue(destinationId)).thenReturn(poisWithCoords());

        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        when(mockRestClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString(), any(Object[].class))).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenReturn("{ \"status\": \"REQUEST_DENIED\", \"results\": [] }");

        PlacesService service = new PlacesService(FAKE_API_KEY, mockRestClient, destinationRepo, poiRepo);

        NeighborhoodSuggestionsDTO result = service.suggestNeighborhoods(destinationId, BudgetTier.MODERATE);

        assertTrue(result.configured());
        assertTrue(result.neighborhoods().isEmpty());
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void suggestNeighborhoods_shouldReturnEmptyList_whenRestClientThrowsNetworkException() {
        when(destinationRepo.findById(destinationId)).thenReturn(Optional.of(destination));
        when(poiRepo.findAllByDestinationIdAndActiveTrue(destinationId)).thenReturn(poisWithCoords());

        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        when(mockRestClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString(), any(Object[].class))).thenThrow(new RuntimeException("timeout"));

        PlacesService service = new PlacesService(FAKE_API_KEY, mockRestClient, destinationRepo, poiRepo);

        NeighborhoodSuggestionsDTO result = service.suggestNeighborhoods(destinationId, BudgetTier.MODERATE);

        assertTrue(result.configured());
        assertTrue(result.neighborhoods().isEmpty());
    }

    // ─── (g) mapeamento de price_level por budgetTier — RED ────────────────
    //
    // Contrato assumido: ECONOMY → minprice=0&maxprice=1; MODERATE →
    // minprice=2&maxprice=2; LUXURY → minprice=3&maxprice=4, na URL/query
    // da Nearby Search.

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void suggestNeighborhoods_shouldUseCorrectPriceRange_forEconomyBudgetTier() {
        assertPriceRangeInUri(BudgetTier.ECONOMY, "minprice=0", "maxprice=1");
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void suggestNeighborhoods_shouldUseCorrectPriceRange_forModerateBudgetTier() {
        assertPriceRangeInUri(BudgetTier.MODERATE, "minprice=2", "maxprice=2");
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void suggestNeighborhoods_shouldUseCorrectPriceRange_forLuxuryBudgetTier() {
        assertPriceRangeInUri(BudgetTier.LUXURY, "minprice=3", "maxprice=4");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void assertPriceRangeInUri(BudgetTier tier, String expectedMinPrice, String expectedMaxPrice) {
        UUID localDestinationId = UUID.randomUUID();
        Destination localDestination = Destination.builder()
            .id(localDestinationId).name("Rio").city("Rio").country("Brasil")
            .slug("rio-" + tier).tagline("x").active(true).build();
        when(destinationRepo.findById(localDestinationId)).thenReturn(Optional.of(localDestination));
        when(poiRepo.findAllByDestinationIdAndActiveTrue(localDestinationId))
            .thenReturn(List.of(
                Poi.builder().id(UUID.randomUUID()).destination(localDestination).title("P1")
                    .category(PoiCategory.MUSEUM).budgetTier(BudgetTier.MODERATE).active(true)
                    .latitude(BigDecimal.valueOf(-22.9)).longitude(BigDecimal.valueOf(-43.2)).build()
            ));

        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        when(mockRestClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(contains(expectedMinPrice), any(Object[].class))).thenReturn(headersSpec);
        when(uriSpec.uri(contains(expectedMaxPrice), any(Object[].class))).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenReturn(ZERO_RESULTS_JSON);

        PlacesService service = new PlacesService(FAKE_API_KEY, mockRestClient, destinationRepo, poiRepo);
        service.suggestNeighborhoods(localDestinationId, tier);

        verify(uriSpec).uri(contains(expectedMinPrice), any(Object[].class));
        verify(uriSpec).uri(contains(expectedMaxPrice), any(Object[].class));
    }
}
