package luvoygroup.voyage.features.maps;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * fetchPhotoUrl acessado diretamente via construtor package-private (ver
 * comentário equivalente em PlacesServiceTest) — mocka a cadeia
 * RestClient.get().uri(...).retrieve().body(String.class) pras duas
 * chamadas sequenciais (busca por texto -> resumo da página).
 */
class WikimediaServiceTest {

    @SuppressWarnings({"unchecked", "rawtypes"})
    private WikimediaService serviceReturning(String... jsonResponses) {
        RestClient mockRestClient = mock(RestClient.class);
        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        when(mockRestClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString(), any(Object[].class))).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenReturn(
            jsonResponses[0], java.util.Arrays.copyOfRange(jsonResponses, 1, jsonResponses.length)
        );
        return new WikimediaService(mockRestClient);
    }

    private static final String SEARCH_WITH_RESULT = """
        {"query": {"search": [{"title": "Eiffel Tower"}]}}
        """;

    private static final String SEARCH_EMPTY = """
        {"query": {"search": []}}
        """;

    private static final String SUMMARY_WITH_THUMBNAIL = """
        {
          "thumbnail": {"source": "https://upload.wikimedia.org/thumb.jpg"},
          "originalimage": {"source": "https://upload.wikimedia.org/original.jpg"}
        }
        """;

    private static final String SUMMARY_WITHOUT_THUMBNAIL_WITH_ORIGINAL = """
        {"originalimage": {"source": "https://upload.wikimedia.org/original.jpg"}}
        """;

    private static final String SUMMARY_WITHOUT_ANY_IMAGE = """
        {"title": "Eiffel Tower"}
        """;

    @Test
    void fetchPhotoUrl_returnsThumbnail_whenSearchFindsPageAndSummaryHasThumbnail() {
        WikimediaService service = serviceReturning(SEARCH_WITH_RESULT, SUMMARY_WITH_THUMBNAIL);

        Optional<String> result = service.fetchPhotoUrl("Torre Eiffel", "Paris");

        assertThat(result).contains("https://upload.wikimedia.org/thumb.jpg");
    }

    @Test
    void fetchPhotoUrl_fallsBackToOriginalImage_whenSummaryHasNoThumbnail() {
        WikimediaService service =
            serviceReturning(SEARCH_WITH_RESULT, SUMMARY_WITHOUT_THUMBNAIL_WITH_ORIGINAL);

        Optional<String> result = service.fetchPhotoUrl("Torre Eiffel", "Paris");

        assertThat(result).contains("https://upload.wikimedia.org/original.jpg");
    }

    @Test
    void fetchPhotoUrl_returnsEmpty_whenSummaryHasNoImageAtAll() {
        WikimediaService service = serviceReturning(SEARCH_WITH_RESULT, SUMMARY_WITHOUT_ANY_IMAGE);

        Optional<String> result = service.fetchPhotoUrl("Torre Eiffel", "Paris");

        assertThat(result).isEmpty();
    }

    @Test
    void fetchPhotoUrl_returnsEmpty_whenSearchFindsNoPage() {
        WikimediaService service = serviceReturning(SEARCH_EMPTY);

        Optional<String> result = service.fetchPhotoUrl("Restaurante Qualquer da Esquina", "São Paulo");

        assertThat(result).isEmpty();
    }

    @Test
    void fetchPhotoUrl_returnsEmpty_neverThrows_onNetworkError() {
        RestClient mockRestClient = mock(RestClient.class);
        when(mockRestClient.get()).thenThrow(new RuntimeException("timeout simulado"));
        WikimediaService service = new WikimediaService(mockRestClient);

        Optional<String> result = service.fetchPhotoUrl("Torre Eiffel", "Paris");

        assertThat(result).isEmpty();
    }

    @Test
    void fetchPhotoUrl_returnsEmpty_onMalformedJson() {
        WikimediaService service = serviceReturning("isso não é json válido");

        Optional<String> result = service.fetchPhotoUrl("Torre Eiffel", "Paris");

        assertThat(result).isEmpty();
    }

    // ─── Rate limit (gotcha real de 2026-10-22, ver Javadoc da classe): a 1ª
    // versão não tinha atraso nenhum entre chamadas e foi bloqueada pela
    // Wikimedia depois de poucos minutos (só 26 de 7307 POIs enriquecidos).
    // Este teste garante que o atraso configurado é respeitado de verdade. ─

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void fetchPhotoUrl_respectsConfiguredThrottle_betweenEachHttpCall() {
        RestClient mockRestClient = mock(RestClient.class);
        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        when(mockRestClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString(), any(Object[].class))).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenReturn(SEARCH_WITH_RESULT, SUMMARY_WITH_THUMBNAIL);

        long throttleMs = 60;
        WikimediaService service = new WikimediaService(mockRestClient, throttleMs);

        long start = System.currentTimeMillis();
        service.fetchPhotoUrl("Torre Eiffel", "Paris");
        long elapsed = System.currentTimeMillis() - start;

        // 2 chamadas HTTP (busca + resumo) -> pelo menos 2x o throttle
        assertThat(elapsed).isGreaterThanOrEqualTo(throttleMs * 2);
    }
}
