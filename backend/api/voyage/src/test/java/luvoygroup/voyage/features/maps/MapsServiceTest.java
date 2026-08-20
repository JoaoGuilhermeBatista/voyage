package luvoygroup.voyage.features.maps;

import luvoygroup.voyage.features.maps.dto.DirectionsResponseDTO;
import luvoygroup.voyage.features.maps.dto.EmbedUrlResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Testes unitários "standalone" de MapsService — NÃO usam @WebMvcTest nem
 * sobem contexto Spring; MapsService é instanciado diretamente via seu
 * construtor package-private (String apiKey, RestClient restClient), o
 * mesmo padrão usado para permitir injetar um RestClient mockado sem
 * depender de infraestrutura HTTP real (o único outro serviço externo do
 * projeto, CurrencyService, ainda não tem um teste unitário equivalente —
 * este arquivo estabelece o padrão para a feature Maps).
 *
 * IMPORTANTE (estado atual — RED por design): MapsService.getDirections e
 * getEmbedUrl são STUBS que lançam UnsupportedOperationException (ver
 * MapsService.java) até o próximo agente implementar a lógica real. Os
 * testes abaixo chamam os métodos diretamente (sem assertThrows) e
 * afirmam o comportamento DESEJADO — por isso devem FALHAR agora (a
 * exceção do stub interrompe o teste antes das asserções) e passar a
 * compilar/rodar em verde assim que a implementação real existir. Não
 * envolvemos as chamadas em assertThrows(UnsupportedOperationException)
 * porque isso inverteria o sinal de TDD (o teste passaria hoje e
 * quebraria quando a feature fosse implementada).
 *
 * Não usamos @ExtendWith(MockitoExtension.class) aqui de propósito: sua
 * verificação de "unnecessary stubbing" (Strictness.STRICT_STUBS) marcaria
 * como erro os stubs do RestClient no cenário (b) — que hoje nunca são
 * exercitados, já que o stub de MapsService lança antes de tocar no
 * RestClient. Mocks criados via Mockito.mock() direto não sofrem essa
 * checagem.
 */
class MapsServiceTest {

    private static final String FAKE_API_KEY = "fake-google-maps-key-for-tests";

    // JSON de exemplo no shape real da resposta da Google Directions API
    // (https://maps.googleapis.com/maps/api/directions/json), reduzido aos
    // campos que a implementação precisa mapear.
    private static final String SAMPLE_DIRECTIONS_JSON = """
        {
          "status": "OK",
          "routes": [
            {
              "legs": [
                {
                  "distance": { "text": "5.2 km", "value": 5200 },
                  "duration": { "text": "12 mins", "value": 720 }
                }
              ]
            }
          ]
        }
        """;

    private RestClient mockRestClient;

    @BeforeEach
    void setUp() {
        mockRestClient = mock(RestClient.class);
    }

    // ─── (a) getDirections — api-key não configurada → configured=false, sem chamada HTTP ─

    @Test
    void getDirections_shouldReturnNotConfigured_andNeverCallRestClient_whenApiKeyIsBlank() {
        MapsService service = new MapsService("not-configured", mockRestClient);

        DirectionsResponseDTO result = service.getDirections(-23.5505, -46.6333, -22.9068, -43.1729);

        assertFalse(result.configured());
        assertNull(result.distanceMeters());
        assertNull(result.durationSeconds());
        assertNull(result.distanceText());
        assertNull(result.durationText());
        verifyNoInteractions(mockRestClient);
    }

    // ─── (b) getDirections — api-key configurada → chama a API real e mapeia a resposta ─
    //
    // Contrato assumido (a confirmar/ajustar pelo próximo agente): a
    // implementação chama restClient.get().uri(...).retrieve().body(String.class)
    // e faz o parse do JSON cru da Directions API, extraindo
    // routes[0].legs[0].distance/duration. Este mock documenta esse
    // contrato; hoje é inerte (o stub lança antes de tocar no RestClient).

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void getDirections_shouldCallGoogleApiAndMapResponse_whenApiKeyIsConfigured() {
        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(mockRestClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString(), any(Object[].class))).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenReturn(SAMPLE_DIRECTIONS_JSON);

        MapsService service = new MapsService(FAKE_API_KEY, mockRestClient);

        DirectionsResponseDTO result = service.getDirections(-23.5505, -46.6333, -22.9068, -43.1729);

        assertTrue(result.configured());
        assertEquals(5200.0, result.distanceMeters());
        assertEquals(720, result.durationSeconds());
        assertEquals("5.2 km", result.distanceText());
        assertEquals("12 mins", result.durationText());
    }

    // ─── (c) getEmbedUrl — api-key não configurada → configured=false, sem chamada HTTP ─

    @Test
    void getEmbedUrl_shouldReturnNotConfigured_andNeverCallRestClient_whenApiKeyIsBlank() {
        MapsService service = new MapsService("not-configured", mockRestClient);

        EmbedUrlResponseDTO result = service.getEmbedUrl(-23.5505, -46.6333);

        assertFalse(result.configured());
        assertNull(result.embedUrl());
        verifyNoInteractions(mockRestClient);
    }

    // ─── (d) getEmbedUrl — api-key configurada → monta a URL (sem chamada HTTP) ─
    //
    // Ao contrário de getDirections, a Embed API não precisa de uma chamada
    // HTTP no backend: é só montar a URL com a key e devolver para o app
    // renderizar (ex.: num WebView/iframe). Por isso verificamos
    // verifyNoInteractions mesmo no caso configured=true.

    @Test
    void getEmbedUrl_shouldBuildUrlWithKeyLatLng_whenApiKeyIsConfigured() {
        MapsService service = new MapsService(FAKE_API_KEY, mockRestClient);

        EmbedUrlResponseDTO result = service.getEmbedUrl(-23.5505, -46.6333);

        assertTrue(result.configured());
        assertTrue(result.embedUrl() != null && result.embedUrl().contains(FAKE_API_KEY));
        assertTrue(result.embedUrl() != null && result.embedUrl().contains("-23.5505"));
        assertTrue(result.embedUrl() != null && result.embedUrl().contains("-46.6333"));
        verifyNoInteractions(mockRestClient);
    }
}
