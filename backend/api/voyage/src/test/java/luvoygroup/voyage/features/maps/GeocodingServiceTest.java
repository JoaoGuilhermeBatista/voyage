package luvoygroup.voyage.features.maps;

import luvoygroup.voyage.features.maps.dto.LatLng;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Testes unitários "standalone" de GeocodingService — mesmo padrão de
 * MapsServiceTest (ver comentário no topo daquele arquivo): NÃO usa
 * @ExtendWith(MockitoExtension.class) de propósito, e GeocodingService é
 * instanciado diretamente via seu construtor package-private (String
 * apiKey, RestClient restClient).
 *
 * IMPORTANTE (estado atual — RED por design nos testes marcados como tal):
 * a checagem de api key (isConfigured, inline em geocode()) é real e já
 * funciona hoje. O restante de geocode() — a chamada à Google Geocoding
 * API e o parsing da resposta — é um STUB que lança
 * UnsupportedOperationException (ver GeocodingService.java) até o próximo
 * agente implementar a lógica real. Os testes que dependem disso chamam
 * geocode diretamente (sem assertThrows) e afirmam o comportamento
 * DESEJADO — por isso devem FALHAR agora e passar a verde quando a
 * implementação real existir.
 */
class GeocodingServiceTest {

    private static final String FAKE_API_KEY = "fake-google-geocoding-key-for-tests";

    private static final String SAMPLE_GEOCODE_JSON = """
        {
          "status": "OK",
          "results": [
            {
              "formatted_address": "Bela Vista, São Paulo - SP, Brasil",
              "geometry": {
                "location": { "lat": -23.5610, "lng": -46.6560 }
              }
            }
          ]
        }
        """;

    private static final String ZERO_RESULTS_JSON = """
        { "status": "ZERO_RESULTS", "results": [] }
        """;

    private RestClient mockRestClient;

    @BeforeEach
    void setUp() {
        mockRestClient = mock(RestClient.class);
    }

    // ─── (a) api-key não configurada → Optional.empty(), sem chamada HTTP ─
    // Este cenário é mecânico (isConfigured é checado de verdade, inline,
    // antes do trecho stub) — já é verde hoje.

    @Test
    void geocode_shouldReturnEmpty_andNeverCallRestClient_whenApiKeyIsBlank() {
        GeocodingService service = new GeocodingService("not-configured", mockRestClient);

        Optional<LatLng> result = service.geocode("Bela Vista, São Paulo, Brasil");

        assertTrue(result.isEmpty());
        verifyNoInteractions(mockRestClient);
    }

    // ─── (b) resposta OK com resultado → LatLng correto — RED ─────────────

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void geocode_shouldReturnLatLng_whenResponseStatusIsOkWithResults() {
        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        when(mockRestClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString(), any(Object[].class))).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenReturn(SAMPLE_GEOCODE_JSON);

        GeocodingService service = new GeocodingService(FAKE_API_KEY, mockRestClient);

        Optional<LatLng> result = service.geocode("Bela Vista, São Paulo, Brasil");

        assertTrue(result.isPresent());
        assertEquals(-23.5610, result.get().lat());
        assertEquals(-46.6560, result.get().lng());
    }

    // ─── (c) ZERO_RESULTS → Optional.empty() — RED ────────────────────────

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void geocode_shouldReturnEmpty_whenResponseStatusIsZeroResults() {
        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        when(mockRestClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString(), any(Object[].class))).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenReturn(ZERO_RESULTS_JSON);

        GeocodingService service = new GeocodingService(FAKE_API_KEY, mockRestClient);

        Optional<LatLng> result = service.geocode("Endereço inexistente, Marte");

        assertTrue(result.isEmpty());
    }

    // ─── (d) status de erro (ex.: REQUEST_DENIED) → Optional.empty() — RED ─

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void geocode_shouldReturnEmpty_whenResponseStatusIsError() {
        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        when(mockRestClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString(), any(Object[].class))).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenReturn("{ \"status\": \"REQUEST_DENIED\", \"results\": [] }");

        GeocodingService service = new GeocodingService(FAKE_API_KEY, mockRestClient);

        Optional<LatLng> result = service.geocode("Bela Vista, São Paulo, Brasil");

        assertTrue(result.isEmpty());
    }

    // ─── (e) exceção de rede → Optional.empty(), não lança — RED ──────────

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void geocode_shouldReturnEmpty_whenRestClientThrowsNetworkException() {
        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        when(mockRestClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString(), any(Object[].class))).thenThrow(new RuntimeException("timeout"));

        GeocodingService service = new GeocodingService(FAKE_API_KEY, mockRestClient);

        Optional<LatLng> result = service.geocode("Bela Vista, São Paulo, Brasil");

        assertTrue(result.isEmpty());
    }

    // ─── reverseGeocodeToNeighborhood ──────────────────────────────────────
    //
    // IMPORTANTE (estado atual — RED por design nos testes marcados como
    // tal): a checagem de api key (isConfigured) é real e já funciona hoje
    // — o teste (f) abaixo é verde. O restante —  a chamada à Google
    // Geocoding API reversa e o parsing de address_components — é um STUB
    // que lança UnsupportedOperationException (ver
    // GeocodingService#reverseGeocodeToNeighborhood) até o próximo agente
    // implementar a lógica real. Os testes (g)-(k) chamam o método
    // diretamente (sem assertThrows) e afirmam o comportamento DESEJADO —
    // por isso devem FALHAR agora e passar a verde quando a implementação
    // real existir.

    // São Paulo: possui sublocality_level_1 → deve preferir esse componente.
    private static final String SAO_PAULO_REVERSE_GEOCODE_JSON = """
        {
          "status": "OK",
          "results": [
            {
              "formatted_address": "Av. Paulista, Bela Vista, São Paulo - SP, Brasil",
              "address_components": [
                { "long_name": "Avenida Paulista", "short_name": "Av. Paulista", "types": ["route"] },
                { "long_name": "Bela Vista", "short_name": "Bela Vista", "types": ["sublocality_level_1", "sublocality", "political"] },
                { "long_name": "São Paulo", "short_name": "São Paulo", "types": ["administrative_area_level_2", "political"] },
                { "long_name": "Brasil", "short_name": "BR", "types": ["country", "political"] }
              ],
              "geometry": { "location": { "lat": -23.5610, "lng": -46.6560 } }
            }
          ]
        }
        """;

    // Barcelona: só tem "neighborhood" (sem sublocality) → deve usar esse.
    private static final String BARCELONA_REVERSE_GEOCODE_JSON = """
        {
          "status": "OK",
          "results": [
            {
              "formatted_address": "Eixample, Barcelona, Espanha",
              "address_components": [
                { "long_name": "Eixample", "short_name": "Eixample", "types": ["neighborhood", "political"] },
                { "long_name": "Barcelona", "short_name": "Barcelona", "types": ["locality", "political"] },
                { "long_name": "Espanha", "short_name": "ES", "types": ["country", "political"] }
              ],
              "geometry": { "location": { "lat": 41.3888, "lng": 2.1590 } }
            }
          ]
        }
        """;

    // Paris: sem sublocality_level_1/sublocality/neighborhood — só locality/country.
    private static final String PARIS_REVERSE_GEOCODE_JSON_NO_MATCH = """
        {
          "status": "OK",
          "results": [
            {
              "formatted_address": "Rue de Rivoli, Paris, França",
              "address_components": [
                { "long_name": "Rue de Rivoli", "short_name": "Rue de Rivoli", "types": ["route"] },
                { "long_name": "Paris", "short_name": "Paris", "types": ["locality", "political"] },
                { "long_name": "França", "short_name": "FR", "types": ["country", "political"] }
              ],
              "geometry": { "location": { "lat": 48.8566, "lng": 2.3522 } }
            }
          ]
        }
        """;

    // ─── (f) api-key não configurada → Optional.empty(), sem chamada HTTP — verde ─

    @Test
    void reverseGeocodeToNeighborhood_shouldReturnEmpty_andNeverCallRestClient_whenApiKeyIsBlank() {
        GeocodingService service = new GeocodingService("not-configured", mockRestClient);

        Optional<String> result = service.reverseGeocodeToNeighborhood(-23.5610, -46.6560);

        assertTrue(result.isEmpty());
        verifyNoInteractions(mockRestClient);
    }

    // ─── (g) sublocality_level_1 presente → retorna o long_name certo — RED ─

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void reverseGeocodeToNeighborhood_shouldReturnSublocalityLevel1_whenPresent() {
        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        when(mockRestClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString(), any(Object[].class))).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenReturn(SAO_PAULO_REVERSE_GEOCODE_JSON);

        GeocodingService service = new GeocodingService(FAKE_API_KEY, mockRestClient);

        Optional<String> result = service.reverseGeocodeToNeighborhood(-23.5610, -46.6560);

        assertTrue(result.isPresent());
        assertEquals("Bela Vista", result.get());
    }

    // ─── (h) só "neighborhood" (sem sublocality) → retorna esse — RED ──────

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void reverseGeocodeToNeighborhood_shouldReturnNeighborhoodType_whenNoSublocalityPresent() {
        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        when(mockRestClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString(), any(Object[].class))).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenReturn(BARCELONA_REVERSE_GEOCODE_JSON);

        GeocodingService service = new GeocodingService(FAKE_API_KEY, mockRestClient);

        Optional<String> result = service.reverseGeocodeToNeighborhood(41.3888, 2.1590);

        assertTrue(result.isPresent());
        assertEquals("Eixample", result.get());
    }

    // ─── (i) nenhum dos 3 types presente (ex: Paris) → Optional.empty() — RED ─

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void reverseGeocodeToNeighborhood_shouldReturnEmpty_whenNoRecognizedComponentTypePresent() {
        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        when(mockRestClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString(), any(Object[].class))).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenReturn(PARIS_REVERSE_GEOCODE_JSON_NO_MATCH);

        GeocodingService service = new GeocodingService(FAKE_API_KEY, mockRestClient);

        Optional<String> result = service.reverseGeocodeToNeighborhood(48.8566, 2.3522);

        assertTrue(result.isEmpty());
    }

    // ─── (j) status de erro (ex.: REQUEST_DENIED) → Optional.empty() — RED ─

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void reverseGeocodeToNeighborhood_shouldReturnEmpty_whenResponseStatusIsError() {
        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        when(mockRestClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString(), any(Object[].class))).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenReturn("{ \"status\": \"REQUEST_DENIED\", \"results\": [] }");

        GeocodingService service = new GeocodingService(FAKE_API_KEY, mockRestClient);

        Optional<String> result = service.reverseGeocodeToNeighborhood(-23.5610, -46.6560);

        assertTrue(result.isEmpty());
    }

    // ─── (k) exceção de rede → Optional.empty(), não lança — RED ───────────

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void reverseGeocodeToNeighborhood_shouldReturnEmpty_whenRestClientThrowsNetworkException() {
        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        when(mockRestClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString(), any(Object[].class))).thenThrow(new RuntimeException("timeout"));

        GeocodingService service = new GeocodingService(FAKE_API_KEY, mockRestClient);

        Optional<String> result = service.reverseGeocodeToNeighborhood(-23.5610, -46.6560);

        assertTrue(result.isEmpty());
    }
}
