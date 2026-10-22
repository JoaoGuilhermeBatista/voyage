package luvoygroup.voyage.features.maps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Optional;

/**
 * Busca foto de POI via Wikipedia pública (en.wikipedia.org) — alternativa
 * SEM CUSTO ao Google Places (pago, ver {@link PlacesService#fetchPhotoUrl}),
 * usada enquanto o orçamento pra API paga não está aprovado (2026-10-22).
 *
 * Cobertura parcial por natureza: só acha foto pra POIs que têm artigo na
 * Wikipedia — landmarks, museus, parques, praias famosas. Restaurante,
 * loja ou balada local normalmente não têm artigo, então ficam sem foto
 * até o Google Places (pago) entrar como complemento no futuro.
 *
 * Wikipedia em inglês (não português): a foto em si não depende de idioma,
 * e a cobertura de imagens do en.wikipedia.org é mais ampla pra marcos
 * turísticos globais do que a versão em português.
 *
 * Fluxo: busca por texto (nome + cidade) → pega o primeiro resultado →
 * busca o resumo da página (REST API) → extrai a foto de capa (thumbnail,
 * com fallback pra originalimage). Nunca lança exceção — qualquer falha
 * (sem resultado, erro de rede, página sem imagem) vira
 * {@code Optional.empty()}, logado como warning — mesmo contrato de
 * {@link PlacesService#fetchPhotoUrl}.
 */
@Service
@Slf4j
public class WikimediaService {

    private static final String SEARCH_URL_TEMPLATE =
        "https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch={query}&srlimit=1&format=json";
    private static final String SUMMARY_URL_TEMPLATE =
        "https://en.wikipedia.org/api/rest_v1/page/summary/{title}";

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public WikimediaService() {
        // User-Agent identificado — exigido pela política de uso da API
        // pública da Wikimedia (requisições anônimas sem User-Agent podem
        // ser limitadas/bloqueadas).
        this(RestClient.builder()
            .defaultHeader("User-Agent", "LuvoyApp/1.0 (contato admin via app)")
            .build());
    }

    /**
     * Construtor package-private, usado pelos testes para injetar um
     * {@link RestClient} mockado sem precisar subir o contexto Spring.
     */
    WikimediaService(RestClient restClient) {
        this.restClient = restClient;
    }

    public Optional<String> fetchPhotoUrl(String title, String city) {
        try {
            String searchJson = restClient.get()
                .uri(SEARCH_URL_TEMPLATE, title + ", " + city)
                .retrieve()
                .body(String.class);

            JsonNode searchResults = objectMapper.readTree(searchJson).path("query").path("search");
            if (!searchResults.isArray() || searchResults.isEmpty()) {
                return Optional.empty();
            }
            String pageTitle = searchResults.get(0).path("title").asText(null);
            if (pageTitle == null) {
                return Optional.empty();
            }

            String summaryJson = restClient.get()
                .uri(SUMMARY_URL_TEMPLATE, pageTitle)
                .retrieve()
                .body(String.class);

            JsonNode summaryRoot = objectMapper.readTree(summaryJson);
            String thumbnail = summaryRoot.path("thumbnail").path("source").asText(null);
            if (thumbnail != null) {
                return Optional.of(thumbnail);
            }
            String original = summaryRoot.path("originalimage").path("source").asText(null);
            return Optional.ofNullable(original);
        } catch (Exception e) {
            log.warn("Erro ao buscar foto na Wikipedia para '{}' ({})", title, city, e);
            return Optional.empty();
        }
    }
}
