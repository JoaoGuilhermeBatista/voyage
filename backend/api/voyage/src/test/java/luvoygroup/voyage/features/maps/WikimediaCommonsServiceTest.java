package luvoygroup.voyage.features.maps;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * searchImages acessado via construtor package-private (mesmo padrão de
 * WikimediaServiceTest) — mocka a cadeia RestClient.get().uri(...)
 * .retrieve().body(String.class) pra 1 chamada só (busca de arquivos no
 * namespace File:, sem a 2ª chamada de resumo que WikimediaService tem).
 */
class WikimediaCommonsServiceTest {

    @SuppressWarnings({"unchecked", "rawtypes"})
    private WikimediaCommonsService serviceReturning(String json) {
        RestClient mockRestClient = mock(RestClient.class);
        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        when(mockRestClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString(), any(Object[].class))).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenReturn(json);
        return new WikimediaCommonsService(mockRestClient);
    }

    private static final String MIXED_RESULTS = """
        {"query": {"pages": {
          "1": {"title": "File:Eiffel Tower view.jpg",
            "imageinfo": [{"url": "https://upload.wikimedia.org/eiffel1.jpg", "width": 1600, "height": 1200}]},
          "2": {"title": "File:Paris location map.svg",
            "imageinfo": [{"url": "https://upload.wikimedia.org/map.svg", "width": 2000, "height": 2000}]},
          "3": {"title": "File:Eiffel Tower icon.png",
            "imageinfo": [{"url": "https://upload.wikimedia.org/icon.png", "width": 64, "height": 64}]},
          "4": {"title": "File:Eiffel Tower night.jpg",
            "imageinfo": [{"url": "https://upload.wikimedia.org/eiffel2.jpg", "width": 1800, "height": 1200}]}
        }}}
        """;

    private static final String NO_RESULTS = """
        {"query": {"pages": {}}}
        """;

    @Test
    void searchImages_returnsOnlyRealPhotos_filteringMapsAndIcons() {
        WikimediaCommonsService service = serviceReturning(MIXED_RESULTS);

        List<String> result = service.searchImages("Torre Eiffel, Paris", 10);

        assertThat(result)
            .contains("https://upload.wikimedia.org/eiffel1.jpg", "https://upload.wikimedia.org/eiffel2.jpg")
            .doesNotContain("https://upload.wikimedia.org/map.svg", "https://upload.wikimedia.org/icon.png");
    }

    @Test
    void searchImages_respectsLimit() {
        WikimediaCommonsService service = serviceReturning(MIXED_RESULTS);

        List<String> result = service.searchImages("Torre Eiffel, Paris", 1);

        assertThat(result).hasSize(1);
    }

    @Test
    void searchImages_returnsEmptyList_whenNoPagesFound() {
        WikimediaCommonsService service = serviceReturning(NO_RESULTS);

        List<String> result = service.searchImages("Lugar Inexistente, Nenhum País", 2);

        assertThat(result).isEmpty();
    }

    @Test
    void searchImages_returnsEmptyList_neverThrows_onNetworkError() {
        RestClient mockRestClient = mock(RestClient.class);
        when(mockRestClient.get()).thenThrow(new RuntimeException("timeout simulado"));
        WikimediaCommonsService service = new WikimediaCommonsService(mockRestClient);

        List<String> result = service.searchImages("Torre Eiffel, Paris", 2);

        assertThat(result).isEmpty();
    }

    @Test
    void searchImages_returnsEmptyList_onMalformedJson() {
        WikimediaCommonsService service = serviceReturning("isso não é json válido");

        List<String> result = service.searchImages("Torre Eiffel, Paris", 2);

        assertThat(result).isEmpty();
    }

    // ─── Mesmo gotcha de rate limit já documentado em WikimediaService — este
    // service reaproveita o mesmo atraso antes de cada chamada HTTP. ─────────

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void searchImages_respectsConfiguredThrottle() {
        RestClient mockRestClient = mock(RestClient.class);
        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        when(mockRestClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString(), any(Object[].class))).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenReturn(MIXED_RESULTS);

        long throttleMs = 60;
        WikimediaCommonsService service = new WikimediaCommonsService(mockRestClient, throttleMs);

        long start = System.currentTimeMillis();
        service.searchImages("Torre Eiffel, Paris", 2);
        long elapsed = System.currentTimeMillis() - start;

        assertThat(elapsed).isGreaterThanOrEqualTo(throttleMs);
    }
}
