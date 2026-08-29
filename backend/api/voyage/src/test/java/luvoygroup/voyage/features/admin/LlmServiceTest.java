package luvoygroup.voyage.features.admin;

import luvoygroup.voyage.domain.Poi;
import luvoygroup.voyage.domain.enums.BudgetTier;
import luvoygroup.voyage.domain.enums.PoiCategory;
import luvoygroup.voyage.features.admin.dto.PoiEnrichmentClassification;
import luvoygroup.voyage.features.admin.dto.PoisWrapper;
import luvoygroup.voyage.features.maps.dto.NeighborhoodRecommendation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testes unitários "standalone" de LlmService — mesmo padrão estabelecido em
 * ItineraryTemplateServiceTest: NÃO usa @ExtendWith(MockitoExtension.class)
 * de propósito, mocks via Mockito.mock() direto, service instanciado via
 * construtor gerado pelo Lombok (@RequiredArgsConstructor).
 *
 * ESTADO ATUAL (RED por design): o prompt de LlmService#generatePois ainda
 * não menciona a categoria LANDMARK nem pede que o LLM marque POIs
 * imperdíveis (mustSee) — essa é a melhoria de prompt (lógica de negócio)
 * que um próximo agente vai implementar. Os testes abaixo capturam o prompt
 * de verdade enviado ao ChatClient e afirmam o conteúdo DESEJADO — por isso
 * devem FALHAR agora e passar a verde assim que o prompt for atualizado.
 */
class LlmServiceTest {

    private ChatClient chatClient;
    private LlmService service;

    @BeforeEach
    void setUp() {
        chatClient = mock(ChatClient.class);
        service = new LlmService(chatClient);
    }

    private String capturePrompt() {
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(promptCaptor.capture())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.entity(PoisWrapper.class)).thenReturn(new PoisWrapper(java.util.List.of()));

        service.generatePois("Copenhagen", "Dinamarca");

        return promptCaptor.getValue();
    }

    // ─── generatePois deve aceitar LANDMARK como categoria válida ────────────

    @Test
    void generatePois_shouldIncludeLandmarkCategory_inPrompt() {
        String prompt = capturePrompt();

        assertTrue(prompt.contains("LANDMARK"),
            "O prompt deve listar LANDMARK entre as categorias aceitas, para que marcos como " +
            "'A Pequena Sereia' ou 'Palácio de Amalienborg' não sejam mais categorizados errado " +
            "nem descartados pelo guard-rail de PoiCategory.valueOf(...)");
    }

    // ─── generatePois deve pedir a chave JSON exata "mustSee" por POI ────────
    //
    // Não é "qualquer palavra razoável": o record GeneratedPoi (ver
    // features/admin/dto/GeneratedPoi.java) tem o campo `Boolean mustSee`,
    // e o ChatClient deserializa a resposta do LLM casando pelo NOME da
    // propriedade JSON. Se o formato de exemplo no prompt não declarar a
    // chave "mustSee" com exatamente esse nome dentro do objeto de cada
    // POI, o LLM nunca vai preencher o campo que o código de fato lê —
    // então o teste exige a chave literal, não uma instrução em prosa.
    @Test
    void generatePois_shouldInstructLlm_toFlagMustSeePois() {
        String prompt = capturePrompt();

        assertTrue(prompt.contains("\"mustSee\""),
            "O formato JSON de exemplo no prompt deve declarar a chave \"mustSee\" (nome exato) " +
            "dentro do objeto de cada POI — é o nome do campo em GeneratedPoi que o ChatClient " +
            "usa para deserializar a resposta do LLM.");
    }

    // ─── generatePois deve pedir a chave JSON exata "catholicChurch" por POI ─
    //
    // Mesmo raciocínio do teste de "mustSee" acima: GeneratedPoi (ver
    // features/admin/dto/GeneratedPoi.java) tem o campo `Boolean
    // catholicChurch`, e o ChatClient casa a resposta do LLM pelo nome da
    // propriedade JSON — então o formato de exemplo do prompt precisa
    // declarar a chave "catholicChurch" com esse nome exato.
    @Test
    void generatePois_shouldInstructLlm_toFlagCatholicChurchPois() {
        String prompt = capturePrompt();

        assertTrue(prompt.contains("\"catholicChurch\""),
            "O formato JSON de exemplo no prompt deve declarar a chave \"catholicChurch\" (nome " +
            "exato) dentro do objeto de cada POI — é o nome do campo em GeneratedPoi que o " +
            "ChatClient usa para deserializar a resposta do LLM.");
    }

    // ─── O prompt deve deixar claro que catholicChurch é EXCLUSIVO de templos católicos ─
    //
    // Contexto de produto: a nova pergunta de religião no onboarding
    // distingue usuários Evangélicos, e o motor de roteiro precisa
    // deprioritzar/excluir especificamente igrejas CATÓLICAS (não marcos
    // históricos em geral, nem templos de outras religiões/denominações).
    // Por isso o prompt precisa instruir o LLM a marcar catholicChurch=false
    // para templos não-católicos, não só listar quando marcar true.
    @Test
    void generatePois_shouldInstructLlm_toExcludeNonCatholicReligiousSites_fromCatholicChurchFlag() {
        String prompt = capturePrompt();

        assertTrue(prompt.toLowerCase().contains("católica") || prompt.toLowerCase().contains("catolica"),
            "O prompt deve mencionar explicitamente 'católica' ao instruir sobre catholicChurch, " +
            "deixando claro que a marcação é exclusiva da Igreja Católica.");
        assertTrue(prompt.toLowerCase().contains("outras religiões") || prompt.toLowerCase().contains("não-católic")
                || prompt.toLowerCase().contains("nao-catolic") || prompt.toLowerCase().contains("não católic"),
            "O prompt deve instruir explicitamente que templos de outras religiões/denominações " +
            "NÃO devem ser marcados como catholicChurch=true, para não confundir 'religioso em geral' " +
            "com 'católico especificamente'.");
    }

    // ─── classifyMustSeeAndLandmarks — construção do prompt ──────────────────
    //
    // Mesmo padrão de classifyCatholicChurches: só testamos a MONTAGEM da
    // chamada ao ChatClient (o prompt de verdade enviado), não a resposta do
    // LLM em si — o método não tem lógica de negócio além de montar o prompt
    // e delegar ao ChatClient.

    private String capturePromptForMustSeeAndLandmarks(List<Poi> pois) {
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(promptCaptor.capture())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.entity(PoiEnrichmentClassification.class))
            .thenReturn(new PoiEnrichmentClassification(List.of(), List.of()));

        service.classifyMustSeeAndLandmarks("Roma", "Itália", pois);

        return promptCaptor.getValue();
    }

    private Poi poiWith(String title, PoiCategory category) {
        return Poi.builder()
            .title(title)
            .category(category)
            .budgetTier(BudgetTier.MODERATE)
            .active(true)
            .build();
    }

    @Test
    void classifyMustSeeAndLandmarks_shouldIncludeCityAndCountry_inPrompt() {
        String prompt = capturePromptForMustSeeAndLandmarks(
            List.of(poiWith("Coliseu", PoiCategory.MUSEUM))
        );

        assertTrue(prompt.contains("Roma"));
        assertTrue(prompt.contains("Itália"));
    }

    @Test
    void classifyMustSeeAndLandmarks_shouldIncludeEachPoiTitle_withCurrentCategory_inPrompt() {
        String prompt = capturePromptForMustSeeAndLandmarks(List.of(
            poiWith("Coliseu", PoiCategory.MUSEUM),
            poiWith("Fontana di Trevi", PoiCategory.PARK)
        ));

        assertTrue(prompt.contains("Coliseu"));
        assertTrue(prompt.contains("MUSEUM"));
        assertTrue(prompt.contains("Fontana di Trevi"));
        assertTrue(prompt.contains("PARK"));
    }

    // ─── o prompt deve pedir as duas chaves JSON exatas ──────────────────────
    //
    // PoiEnrichmentClassification tem os campos `mustSeeTitles` e
    // `landmarkTitles` — o ChatClient deserializa a resposta do LLM casando
    // pelo nome da propriedade JSON, então o formato de exemplo no prompt
    // precisa declarar essas chaves com o nome exato.

    @Test
    void classifyMustSeeAndLandmarks_shouldRequestMustSeeTitlesKey_inPrompt() {
        String prompt = capturePromptForMustSeeAndLandmarks(
            List.of(poiWith("Coliseu", PoiCategory.MUSEUM))
        );

        assertTrue(prompt.contains("\"mustSeeTitles\""));
    }

    @Test
    void classifyMustSeeAndLandmarks_shouldRequestLandmarkTitlesKey_inPrompt() {
        String prompt = capturePromptForMustSeeAndLandmarks(
            List.of(poiWith("Coliseu", PoiCategory.MUSEUM))
        );

        assertTrue(prompt.contains("\"landmarkTitles\""));
    }

    // ─── o prompt deve instruir o LLM a não inventar títulos / permitir listas vazias ─

    @Test
    void classifyMustSeeAndLandmarks_shouldInstructLlm_toNeverInventTitles() {
        String prompt = capturePromptForMustSeeAndLandmarks(
            List.of(poiWith("Coliseu", PoiCategory.MUSEUM))
        );

        assertTrue(prompt.toLowerCase().contains("nunca invente") || prompt.toLowerCase().contains("não invente"),
            "O prompt deve instruir explicitamente que o LLM não deve inventar títulos novos.");
    }

    @Test
    void classifyMustSeeAndLandmarks_shouldCallChatClient_withPoiEnrichmentClassificationEntity() {
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);
        PoiEnrichmentClassification expected =
            new PoiEnrichmentClassification(List.of("Coliseu"), List.of("Fontana di Trevi"));

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.entity(PoiEnrichmentClassification.class)).thenReturn(expected);

        PoiEnrichmentClassification actual = service.classifyMustSeeAndLandmarks(
            "Roma", "Itália", List.of(poiWith("Coliseu", PoiCategory.MUSEUM))
        );

        assertEquals(expected, actual);
    }

    // ─── recommendNeighborhoods — RED, é um STUB (UnsupportedOperationException) ─
    //
    // Mesmo padrão de teste de contrato usado para classifyMustSeeAndLandmarks
    // quando foi criado: chamamos o método real (sem assertThrows) e afirmamos
    // o comportamento DESEJADO — por isso devem FALHAR agora (com
    // UnsupportedOperationException) e passar a verde quando a implementação
    // real existir.

    @Test
    void recommendNeighborhoods_shouldCallChatClient_withNeighborhoodRecommendationListEntity_andReturnItsRecommendations() {
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);
        List<NeighborhoodRecommendation> expected = List.of(
            new NeighborhoodRecommendation("Bela Vista", "Perto de tudo e com ótimo custo-benefício.")
        );

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.entity(luvoygroup.voyage.features.maps.dto.NeighborhoodRecommendationList.class))
            .thenReturn(new luvoygroup.voyage.features.maps.dto.NeighborhoodRecommendationList(expected));

        List<NeighborhoodRecommendation> actual = service.recommendNeighborhoods(
            "São Paulo", "Brasil", List.of("Bela Vista", "Consolação", "Jardins"), BudgetTier.MODERATE
        );

        assertEquals(expected, actual);
    }

    @Test
    void recommendNeighborhoods_shouldIncludeAllCandidateNeighborhoods_inPrompt() {
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(promptCaptor.capture())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.entity(luvoygroup.voyage.features.maps.dto.NeighborhoodRecommendationList.class))
            .thenReturn(new luvoygroup.voyage.features.maps.dto.NeighborhoodRecommendationList(List.of()));

        service.recommendNeighborhoods(
            "São Paulo", "Brasil", List.of("Bela Vista", "Consolação", "Jardins"), BudgetTier.LUXURY
        );

        String prompt = promptCaptor.getValue();
        assertTrue(prompt.contains("Bela Vista"));
        assertTrue(prompt.contains("Consolação"));
        assertTrue(prompt.contains("Jardins"));
        assertTrue(prompt.contains("São Paulo"));
        assertTrue(prompt.contains("Brasil"));
    }

    @Test
    void recommendNeighborhoods_shouldFilterOutHallucinatedNames_notInCandidateList() {
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);

        NeighborhoodRecommendation valid = new NeighborhoodRecommendation("Bela Vista", "Vantagem real.");
        NeighborhoodRecommendation hallucinated = new NeighborhoodRecommendation("Bairro Inventado", "Vantagem inventada.");

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.entity(luvoygroup.voyage.features.maps.dto.NeighborhoodRecommendationList.class))
            .thenReturn(new luvoygroup.voyage.features.maps.dto.NeighborhoodRecommendationList(
                List.of(valid, hallucinated)));

        List<NeighborhoodRecommendation> actual = service.recommendNeighborhoods(
            "São Paulo", "Brasil", List.of("Bela Vista", "Consolação", "Jardins"), BudgetTier.ECONOMY
        );

        assertEquals(List.of(valid), actual);
    }
}
