package luvoygroup.voyage.features.maps;

import luvoygroup.voyage.domain.Destination;
import luvoygroup.voyage.domain.Poi;
import luvoygroup.voyage.domain.enums.BudgetTier;
import luvoygroup.voyage.domain.enums.PoiCategory;
import luvoygroup.voyage.exception.ResourceNotFoundException;
import luvoygroup.voyage.features.admin.LlmService;
import luvoygroup.voyage.features.maps.dto.NeighborhoodRecommendation;
import luvoygroup.voyage.features.maps.dto.NeighborhoodSuggestionsDTO;
import luvoygroup.voyage.repository.DestinationRepository;
import luvoygroup.voyage.repository.PoiRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
 * discoverRealNeighborhoods, hoje inerte), e PlacesService é instanciado
 * diretamente via seu construtor package-private (String apiKey, RestClient
 * restClient, DestinationRepository, PoiRepository, GeocodingService,
 * LlmService).
 *
 * IMPORTANTE (estado atual — RED por design nos testes marcados como tal):
 * PlacesService#discoverRealNeighborhoods é um STUB que lança
 * UnsupportedOperationException (ver PlacesService.java) até o próximo
 * agente implementar a lógica real de chamada à Google Places Nearby
 * Search + reverse geocoding. Os testes que dependem desse método chamam
 * suggestNeighborhoods diretamente (sem assertThrows) e afirmam o
 * comportamento DESEJADO — por isso devem FALHAR agora e passar a verde
 * quando discoverRealNeighborhoods for implementado.
 */
class PlacesServiceTest {

    private static final String FAKE_API_KEY = "fake-google-places-key-for-tests";

    // JSON de exemplo no shape real da resposta da Google Places Nearby
    // Search (sem price_level — o filtro de preço foi removido, ver
    // PlacesService.discoverRealNeighborhoods), com 2 resultados de
    // lodging cujas coordenadas batem com poisWithCoords() só pra manter o
    // exemplo coerente (as coordenadas dos resultados de lodging não
    // precisam bater com as dos POIs do destino de verdade).
    private static final String SAMPLE_NEARBY_SEARCH_JSON = """
        {
          "status": "OK",
          "results": [
            { "name": "Hotel A", "geometry": { "location": { "lat": -23.5610, "lng": -46.6560 } } },
            { "name": "Hotel B", "geometry": { "location": { "lat": -23.5620, "lng": -46.6550 } } }
          ]
        }
        """;

    private DestinationRepository destinationRepo;
    private PoiRepository poiRepo;
    private RestClient mockRestClient;
    private GeocodingService geocodingService;
    private LlmService llmService;
    private UUID destinationId;
    private Destination destination;

    @BeforeEach
    void setUp() {
        destinationRepo = mock(DestinationRepository.class);
        poiRepo = mock(PoiRepository.class);
        mockRestClient = mock(RestClient.class);
        geocodingService = mock(GeocodingService.class);
        llmService = mock(LlmService.class);
        destinationId = UUID.randomUUID();
        destination = Destination.builder()
            .id(destinationId).name("São Paulo").city("São Paulo").country("Brasil")
            .slug("sao-paulo").tagline("Cidade que não para").active(true).build();
    }

    private PlacesService newService() {
        return new PlacesService(FAKE_API_KEY, mockRestClient, destinationRepo, poiRepo, geocodingService, llmService);
    }

    // Stub da cadeia RestClient.get().uri(...).retrieve().body(String.class)
    // pra Nearby Search — necessário pra exercitar discoverRealNeighborhoods
    // de verdade (sem isso, mockRestClient.get() retorna null e a chamada
    // real dá NPE, capturada pelo try/catch e virando lista vazia —
    // o que faz suggestNeighborhoods nunca chegar a chamar o LlmService).
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void stubNearbySearch(String json) {
        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        when(mockRestClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString(), any(Object[].class))).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenReturn(json);
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
        PlacesService service = new PlacesService(
            "not-configured", mockRestClient, destinationRepo, poiRepo, geocodingService, llmService);

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
        PlacesService service = newService();

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

        PlacesService service = newService();

        NeighborhoodSuggestionsDTO result = service.suggestNeighborhoods(destinationId, BudgetTier.MODERATE);

        assertTrue(result.configured());
        assertTrue(result.neighborhoods().isEmpty());
        verifyNoInteractions(mockRestClient);
    }

    // ─── (d) fluxo completo — RED, depende do stub discoverRealNeighborhoods ─
    //
    // Contrato assumido: discoverRealNeighborhoods descobre os candidatos
    // reais (via Nearby Search + reverse geocode, mockado aqui através de
    // GeocodingService), e suggestNeighborhoods delega ao LlmService para
    // produzir a recomendação final — o resultado retornado é exatamente o
    // que o LlmService devolve.

    @Test
    void suggestNeighborhoods_shouldReturnLlmRecommendations_whenFullFlowSucceeds() {
        when(destinationRepo.findById(destinationId)).thenReturn(Optional.of(destination));
        when(poiRepo.findAllByDestinationIdAndActiveTrue(destinationId)).thenReturn(poisWithCoords());
        stubNearbySearch(SAMPLE_NEARBY_SEARCH_JSON);
        when(geocodingService.reverseGeocodeToNeighborhood(-23.5610, -46.6560))
            .thenReturn(Optional.of("Bela Vista"));
        when(geocodingService.reverseGeocodeToNeighborhood(-23.5620, -46.6550))
            .thenReturn(Optional.of("Jardins"));

        List<NeighborhoodRecommendation> expected = List.of(
            new NeighborhoodRecommendation("Bela Vista", "Perto de tudo e com ótimo custo-benefício."),
            new NeighborhoodRecommendation("Jardins", "Bairro sofisticado, ótimo para gastronomia.")
        );
        when(llmService.recommendNeighborhoods(
            eq("São Paulo"), eq("Brasil"), any(), eq(BudgetTier.MODERATE)))
            .thenReturn(expected);

        PlacesService service = newService();

        NeighborhoodSuggestionsDTO result = service.suggestNeighborhoods(destinationId, BudgetTier.MODERATE);

        assertTrue(result.configured());
        assertEquals(expected, result.neighborhoods());
    }

    // ─── (e) cache de descoberta — RED, depende do stub rodar com sucesso ──
    //
    // Chamar suggestNeighborhoods duas vezes com o MESMO destinationId mas
    // budgetTier DIFERENTE não deve repetir a Nearby Search (RestClient)
    // nem os reverse-geocodes, mas DEVE chamar llmService.recommendNeighborhoods
    // de novo para cada budgetTier (a recomendação é por budget).

    @Test
    void suggestNeighborhoods_shouldNotRepeatDiscovery_butShouldCallLlmAgain_forDifferentBudgetTiers() {
        when(destinationRepo.findById(destinationId)).thenReturn(Optional.of(destination));
        when(poiRepo.findAllByDestinationIdAndActiveTrue(destinationId)).thenReturn(poisWithCoords());
        stubNearbySearch(SAMPLE_NEARBY_SEARCH_JSON);
        when(geocodingService.reverseGeocodeToNeighborhood(-23.5610, -46.6560))
            .thenReturn(Optional.of("Bela Vista"));
        when(geocodingService.reverseGeocodeToNeighborhood(-23.5620, -46.6550))
            .thenReturn(Optional.of("Jardins"));
        when(llmService.recommendNeighborhoods(anyString(), anyString(), any(), any()))
            .thenReturn(List.of(new NeighborhoodRecommendation("Bela Vista", "Vantagem qualquer.")));

        PlacesService service = newService();

        service.suggestNeighborhoods(destinationId, BudgetTier.ECONOMY);
        service.suggestNeighborhoods(destinationId, BudgetTier.LUXURY);

        verify(mockRestClient, times(1)).get();
        verify(llmService, times(2)).recommendNeighborhoods(anyString(), anyString(), any(), any());
    }

    // ─── (f) candidatos vazios → configured=true, [], nunca chama o LLM — verde (mecânico) ─
    //
    // Para exercitar exclusivamente a lógica MECÂNICA de suggestNeighborhoods
    // (candidates.isEmpty() → nunca chama o LLM) sem depender do stub
    // discoverRealNeighborhoods (hoje inerte), pré-populamos o discoveryCache
    // privado via reflection com uma lista vazia para este destinationId —
    // isso faz computeIfAbsent NÃO chamar discoverRealNeighborhoods.

    @Test
    void suggestNeighborhoods_shouldReturnEmptyConfigured_andNeverCallLlm_whenDiscoveryReturnsNoCandidates() {
        when(destinationRepo.findById(destinationId)).thenReturn(Optional.of(destination));
        when(poiRepo.findAllByDestinationIdAndActiveTrue(destinationId)).thenReturn(poisWithCoords());

        PlacesService service = newService();
        injectDiscoveryCache(service, destinationId, List.of());

        NeighborhoodSuggestionsDTO result = service.suggestNeighborhoods(destinationId, BudgetTier.MODERATE);

        assertTrue(result.configured());
        assertTrue(result.neighborhoods().isEmpty());
        verifyNoInteractions(llmService);
        verifyNoInteractions(mockRestClient);
    }

    // ─── (g) erro no llmService.recommendNeighborhoods → configured=true, [], não propaga — verde (mecânico) ─
    //
    // Mecânico: o try/catch em torno da chamada ao LlmService já é
    // implementado de verdade. Para chegar até essa chamada sem depender do
    // stub discoverRealNeighborhoods, pré-populamos o discoveryCache via
    // reflection com candidatos não vazios.

    @Test
    void suggestNeighborhoods_shouldReturnEmptyConfigured_andNotPropagate_whenLlmServiceThrows() {
        when(destinationRepo.findById(destinationId)).thenReturn(Optional.of(destination));
        when(poiRepo.findAllByDestinationIdAndActiveTrue(destinationId)).thenReturn(poisWithCoords());
        when(llmService.recommendNeighborhoods(anyString(), anyString(), any(), any()))
            .thenThrow(new RuntimeException("LLM indisponível"));

        PlacesService service = newService();
        injectDiscoveryCache(service, destinationId, List.of("Bela Vista", "Jardins"));

        NeighborhoodSuggestionsDTO result = service.suggestNeighborhoods(destinationId, BudgetTier.MODERATE);

        assertTrue(result.configured());
        assertTrue(result.neighborhoods().isEmpty());
    }

    // ─── (h) resultado vazio por falha transitória NÃO fica cacheado pra sempre ─
    //
    // Bug real suspeitado (registrado em memória de sessão anterior): se a
    // 1ª tentativa de discoverRealNeighborhoods falhar por um motivo
    // transitório (aqui simulado como um RuntimeException do RestClient —
    // capturada pelo try/catch, vira lista vazia), essa lista vazia NÃO
    // pode ficar memoizada pra sempre — a próxima chamada (mesmo
    // destinationId) deve tentar de novo, e se a API responder OK dessa
    // vez, deve retornar sugestões de verdade.

    @Test
    void suggestNeighborhoods_shouldRetryDiscovery_whenFirstAttemptFailedTransiently_insteadOfCachingEmptyForever() {
        when(destinationRepo.findById(destinationId)).thenReturn(Optional.of(destination));
        when(poiRepo.findAllByDestinationIdAndActiveTrue(destinationId)).thenReturn(poisWithCoords());

        // 1ª tentativa: RestClient falha (ex.: timeout de rede) — capturado
        // por discoverRealNeighborhoods, vira lista vazia.
        when(mockRestClient.get()).thenThrow(new RuntimeException("timeout simulado"));

        PlacesService service = newService();
        NeighborhoodSuggestionsDTO first = service.suggestNeighborhoods(destinationId, BudgetTier.MODERATE);
        assertTrue(first.configured());
        assertTrue(first.neighborhoods().isEmpty());

        // 2ª tentativa: agora a API responde normalmente. reset() é
        // necessário antes de re-stubar — com o mock ainda armado pra
        // lançar, o próprio when(mockRestClient.get()) invocaria o método
        // e re-lançaria a exceção antes de conseguir trocar o stub.
        org.mockito.Mockito.reset(mockRestClient);
        stubNearbySearch(SAMPLE_NEARBY_SEARCH_JSON);
        when(geocodingService.reverseGeocodeToNeighborhood(-23.5610, -46.6560))
            .thenReturn(Optional.of("Bela Vista"));
        when(geocodingService.reverseGeocodeToNeighborhood(-23.5620, -46.6550))
            .thenReturn(Optional.of("Jardins"));
        when(llmService.recommendNeighborhoods(anyString(), anyString(), any(), any()))
            .thenReturn(List.of(new NeighborhoodRecommendation("Bela Vista", "Vantagem qualquer.")));

        NeighborhoodSuggestionsDTO second = service.suggestNeighborhoods(destinationId, BudgetTier.MODERATE);

        // reset(mockRestClient) acima zera o histórico de invocações — o
        // que importa aqui é que ELE FOI CHAMADO DE NOVO (retry de
        // verdade), não o total acumulado desde o início do teste.
        verify(mockRestClient, times(1)).get();
        assertFalse(second.neighborhoods().isEmpty());
        assertEquals("Bela Vista", second.neighborhoods().get(0).name());
    }

    // ─── fetchPhotoUrl ──────────────────────────────────────────────────────

    private static final String SAMPLE_FIND_PLACE_JSON = """
        {"status": "OK", "candidates": [{"place_id": "place-123"}]}
        """;

    private static final String SAMPLE_PLACE_DETAILS_JSON = """
        {"status": "OK", "result": {"photos": [{"photo_reference": "ref-abc"}]}}
        """;

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void stubSequentialBodies(String... jsonResponses) {
        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        when(mockRestClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString(), any(Object[].class))).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenReturn(jsonResponses[0],
            java.util.Arrays.copyOfRange(jsonResponses, 1, jsonResponses.length));
    }

    @Test
    void fetchPhotoUrl_shouldReturnEmpty_withoutAnyHttpCall_whenApiKeyIsNotConfigured() {
        Function<String, Optional<String>> redirectResolver = mock(Function.class);
        PlacesService service = new PlacesService(
            "not-configured", mockRestClient, destinationRepo, poiRepo, geocodingService, llmService, redirectResolver);

        Optional<String> result = service.fetchPhotoUrl("Coliseu", "Roma");

        assertTrue(result.isEmpty());
        verifyNoInteractions(mockRestClient);
        verifyNoInteractions(redirectResolver);
    }

    @Test
    void fetchPhotoUrl_shouldReturnFinalRedirectUrl_whenFullFlowSucceeds() {
        stubSequentialBodies(SAMPLE_FIND_PLACE_JSON, SAMPLE_PLACE_DETAILS_JSON);
        Function<String, Optional<String>> redirectResolver = mock(Function.class);
        when(redirectResolver.apply(anyString()))
            .thenReturn(Optional.of("https://lh3.googleusercontent.com/final-photo"));

        PlacesService service = new PlacesService(
            FAKE_API_KEY, mockRestClient, destinationRepo, poiRepo, geocodingService, llmService, redirectResolver);

        Optional<String> result = service.fetchPhotoUrl("Coliseu", "Roma");

        assertEquals(Optional.of("https://lh3.googleusercontent.com/final-photo"), result);
    }

    @Test
    void fetchPhotoUrl_shouldReturnEmpty_andNeverCallDetailsOrRedirectResolver_whenFindPlaceHasNoCandidates() {
        stubSequentialBodies("""
            {"status": "ZERO_RESULTS", "candidates": []}
            """);
        Function<String, Optional<String>> redirectResolver = mock(Function.class);
        PlacesService service = new PlacesService(
            FAKE_API_KEY, mockRestClient, destinationRepo, poiRepo, geocodingService, llmService, redirectResolver);

        Optional<String> result = service.fetchPhotoUrl("Lugar Inexistente", "Roma");

        assertTrue(result.isEmpty());
        verifyNoInteractions(redirectResolver);
    }

    @Test
    void fetchPhotoUrl_shouldReturnEmpty_whenPlaceDetailsHasNoPhotos() {
        stubSequentialBodies(SAMPLE_FIND_PLACE_JSON, """
            {"status": "OK", "result": {"photos": []}}
            """);
        Function<String, Optional<String>> redirectResolver = mock(Function.class);
        PlacesService service = new PlacesService(
            FAKE_API_KEY, mockRestClient, destinationRepo, poiRepo, geocodingService, llmService, redirectResolver);

        Optional<String> result = service.fetchPhotoUrl("Coliseu", "Roma");

        assertTrue(result.isEmpty());
        verifyNoInteractions(redirectResolver);
    }

    @Test
    void fetchPhotoUrl_shouldReturnEmpty_neverPropagating_whenRestClientThrows() {
        when(mockRestClient.get()).thenThrow(new RuntimeException("timeout simulado"));
        Function<String, Optional<String>> redirectResolver = mock(Function.class);
        PlacesService service = new PlacesService(
            FAKE_API_KEY, mockRestClient, destinationRepo, poiRepo, geocodingService, llmService, redirectResolver);

        Optional<String> result = service.fetchPhotoUrl("Coliseu", "Roma");

        assertTrue(result.isEmpty());
    }

    @SuppressWarnings("unchecked")
    private void injectDiscoveryCache(PlacesService service, UUID destId, List<String> candidates) {
        try {
            Field field = PlacesService.class.getDeclaredField("discoveryCache");
            field.setAccessible(true);
            Map<UUID, List<String>> discoveryCache = (Map<UUID, List<String>>) field.get(service);
            discoveryCache.put(destId, candidates);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
