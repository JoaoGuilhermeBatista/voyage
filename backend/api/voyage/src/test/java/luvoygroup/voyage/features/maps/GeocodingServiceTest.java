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
}
