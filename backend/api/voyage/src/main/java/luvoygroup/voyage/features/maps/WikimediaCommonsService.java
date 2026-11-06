package luvoygroup.voyage.features.maps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Busca várias fotos distintas de um lugar via Wikimedia Commons (busca por
 * texto no namespace File:) — SEM CUSTO, complementa {@link WikimediaService}
 * (que só devolve 1 foto por página, a de resumo da Wikipedia) pra permitir
 * fotos adicionais/rotativas por destino (ver luvoy-pendencias.md,
 * 2026-11-02).
 *
 * <p>Busca por palavra-chave no Commons é ruidosa (mapas, bandeiras, brasões,
 * ícones aparecem junto com fotos reais) — {@link #searchImages} filtra por
 * tamanho mínimo (exclui ícones/thumbnails) e por palavras no nome do
 * arquivo que indicam que não é uma foto (mapa/bandeira/ícone/logo/etc.),
 * mas não é curadoria editorial de verdade — vale conferir visualmente uma
 * amostra depois de rodar em lote, mesmo critério já registrado em memória
 * pra curadoria de heroImageUrl.</p>
 */
@Service
@Slf4j
public class WikimediaCommonsService {

    private static final String SEARCH_URL_TEMPLATE =
        "https://commons.wikimedia.org/w/api.php?action=query&generator=search&gsrnamespace=6"
            + "&gsrsearch={query}&gsrlimit={limit}&prop=imageinfo&iiprop=url|size&format=json";

    private static final long THROTTLE_MS = 350;

    /** Tamanho mínimo (maior lado, em px) pra não aceitar ícone/thumbnail. */
    private static final int MIN_DIMENSION = 800;

    private static final List<String> JUNK_FILENAME_MARKERS = List.of(
        "map", "flag", "icon", "logo", "coat of arms", "locator", "symbol", "seal", ".svg"
    );

    private final RestClient restClient;
    private final long throttleMs;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public WikimediaCommonsService() {
        this(
            RestClient.builder()
                .defaultHeader(
                    "User-Agent",
                    "LuvoyApp/1.0 (https://github.com/JoaoGuilhermeBatista/luvoy)"
                )
                .build(),
            THROTTLE_MS
        );
    }

    /** Construtor package-private pra teste — ver {@link WikimediaService}. */
    WikimediaCommonsService(RestClient restClient) {
        this(restClient, 0);
    }

    WikimediaCommonsService(RestClient restClient, long throttleMs) {
        this.restClient = restClient;
        this.throttleMs = throttleMs;
    }

    /**
     * Devolve até {@code limit} URLs de fotos reais (filtradas) pra
     * {@code query} — nunca lança exceção, falha vira lista vazia (mesmo
     * contrato de {@link WikimediaService#fetchPhotoUrl}).
     */
    public List<String> searchImages(String query, int limit) {
        try {
            throttle();
            // Busca com folga (3x o limite) porque boa parte dos resultados
            // crus é descartada pelo filtro de qualidade.
            String json = restClient.get()
                .uri(SEARCH_URL_TEMPLATE, query, Math.max(limit * 3, 10))
                .retrieve()
                .body(String.class);

            JsonNode pages = objectMapper.readTree(json).path("query").path("pages");
            if (!pages.isObject()) {
                return List.of();
            }
            List<String> results = new ArrayList<>();
            for (JsonNode page : pages) {
                if (results.size() >= limit) break;
                String url = photoUrlOrNull(page);
                if (url != null) {
                    results.add(url);
                }
            }
            return results;
        } catch (Exception e) {
            log.warn("Erro ao buscar imagens no Wikimedia Commons para '{}'", query, e);
            return List.of();
        }
    }

    /**
     * URL da página de arquivo se ela passar no filtro de qualidade (nome
     * que não indica mapa/ícone/etc. e maior lado ≥ {@link #MIN_DIMENSION});
     * {@code null} caso contrário.
     */
    private String photoUrlOrNull(JsonNode page) {
        if (isJunkFilename(page.path("title").asText(""))) return null;

        JsonNode imageInfoArray = page.path("imageinfo");
        if (!imageInfoArray.isArray() || imageInfoArray.isEmpty()) return null;
        JsonNode imageInfo = imageInfoArray.get(0);

        int width = imageInfo.path("width").asInt(0);
        int height = imageInfo.path("height").asInt(0);
        if (Math.max(width, height) < MIN_DIMENSION) return null;

        return imageInfo.path("url").asText(null);
    }

    private boolean isJunkFilename(String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
        return JUNK_FILENAME_MARKERS.stream().anyMatch(lower::contains);
    }

    private void throttle() {
        if (throttleMs <= 0) return;
        try {
            Thread.sleep(throttleMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
