package luvoygroup.voyage.features.admin;

import luvoygroup.voyage.features.admin.dto.PoisWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;

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
}
