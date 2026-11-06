package luvoygroup.voyage.features.itinerarytemplates;

import luvoygroup.voyage.domain.*;
import luvoygroup.voyage.domain.enums.BudgetTier;
import luvoygroup.voyage.domain.enums.ItinerarySlot;
import luvoygroup.voyage.domain.enums.PoiCategory;
import luvoygroup.voyage.exception.ResourceNotFoundException;
import luvoygroup.voyage.features.itinerary.dto.ItineraryResponseDTO;
import luvoygroup.voyage.features.itinerarytemplates.dto.GeneratedTemplateActivity;
import luvoygroup.voyage.features.itinerarytemplates.dto.GeneratedTemplateContent;
import luvoygroup.voyage.features.itinerarytemplates.dto.ItineraryTemplateDTO;
import luvoygroup.voyage.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitários "standalone" de ItineraryTemplateService — mesmo padrão
 * estabelecido por MapsServiceTest: NÃO usam @WebMvcTest nem sobem contexto
 * Spring; o service é instanciado diretamente via seu construtor gerado
 * pelo Lombok (@RequiredArgsConstructor), injetando mocks Mockito puros
 * para cada colaborador (ChatClient, repositórios).
 *
 * IMPORTANTE (estado atual — RED por design): TODOS os métodos de
 * ItineraryTemplateService são STUBS que lançam UnsupportedOperationException
 * (ver ItineraryTemplateService.java) até um próximo agente implementar a
 * lógica real. Os testes abaixo chamam os métodos diretamente e afirmam o
 * comportamento DESEJADO — por isso devem FALHAR agora (a exceção do stub
 * interrompe o teste antes das asserções, ou faz assertThrows falhar por
 * capturar o tipo errado de exceção) e devem passar a rodar em verde assim
 * que a implementação real existir.
 *
 * Não usamos @ExtendWith(MockitoExtension.class) de propósito, pelo mesmo
 * motivo documentado em MapsServiceTest: vários stubs abaixo (ex.: toda a
 * cadeia fluente do ChatClient no teste de geração) nunca chegam a ser
 * exercitados porque o stub do service lança antes de tocar neles, e a
 * verificação de "unnecessary stubbing" (Strictness.STRICT_STUBS) marcaria
 * isso como erro. Mocks via Mockito.mock() direto não sofrem essa checagem.
 */
class ItineraryTemplateServiceTest {

    private ChatClient chatClient;
    private DestinationRepository destinationRepo;
    private PoiRepository poiRepo;
    private ItineraryTemplateRepository templateRepo;
    private ItineraryRepository itineraryRepo;
    private UserRepository userRepo;

    private ItineraryTemplateService service;

    private final UUID templateId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID destinationId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        chatClient = mock(ChatClient.class);
        destinationRepo = mock(DestinationRepository.class);
        poiRepo = mock(PoiRepository.class);
        templateRepo = mock(ItineraryTemplateRepository.class);
        itineraryRepo = mock(ItineraryRepository.class);
        userRepo = mock(UserRepository.class);

        service = new ItineraryTemplateService(
            chatClient, destinationRepo, poiRepo, templateRepo, itineraryRepo, userRepo
        );
    }

    // ─── (a) adopt — computa endDate e clona os dias/atividades do template em um Itinerary real ─
    //
    // Contrato assumido: endDate = startDate.plusDays(durationDays - 1); cada
    // ItineraryTemplateDay vira um ItineraryDay com date = startDate.plusDays(dayNumber - 1);
    // cada ItineraryTemplateActivity é clonada em uma ItineraryActivity com o
    // MESMO poi/slot/orderInSlot; o retorno tem o MESMO shape de
    // ItineraryService#findById (ItineraryResponseDTO).

    @Test
    void adopt_shouldComputeEndDateAndCloneDaysAndActivities_fromTemplate() {
        Destination destination = Destination.builder()
            .id(destinationId).slug("lisboa").name("Lisboa").heroImageUrl("https://img.example/lisboa.jpg")
            .build();

        ItineraryTemplate template = ItineraryTemplate.builder()
            .id(templateId).destination(destination).title("Lisboa Clássica")
            .description("3 dias de cultura e gastronomia").durationDays(2).budgetTier(BudgetTier.MODERATE)
            .build();

        Poi poi1 = Poi.builder().id(UUID.randomUUID()).destination(destination).title("Torre de Belém")
            .category(PoiCategory.MUSEUM).budgetTier(BudgetTier.MODERATE).build();
        Poi poi2 = Poi.builder().id(UUID.randomUUID()).destination(destination).title("Time Out Market")
            .category(PoiCategory.RESTAURANT).budgetTier(BudgetTier.MODERATE).build();

        ItineraryTemplateDay day1 = ItineraryTemplateDay.builder().id(UUID.randomUUID()).itineraryTemplate(template).dayNumber(1).build();
        ItineraryTemplateActivity act1 = ItineraryTemplateActivity.builder()
            .id(UUID.randomUUID()).itineraryTemplateDay(day1).poi(poi1).slot(ItinerarySlot.MORNING).orderInSlot(0).build();
        day1.setActivities(new ArrayList<>(List.of(act1)));

        ItineraryTemplateDay day2 = ItineraryTemplateDay.builder().id(UUID.randomUUID()).itineraryTemplate(template).dayNumber(2).build();
        ItineraryTemplateActivity act2 = ItineraryTemplateActivity.builder()
            .id(UUID.randomUUID()).itineraryTemplateDay(day2).poi(poi2).slot(ItinerarySlot.AFTERNOON).orderInSlot(0).build();
        day2.setActivities(new ArrayList<>(List.of(act2)));

        template.setDays(new ArrayList<>(List.of(day1, day2)));

        when(templateRepo.findById(templateId)).thenReturn(Optional.of(template));

        User user = User.builder().id(userId).email("viajante@luvoy.com").name("Viajante").build();
        when(userRepo.getReferenceById(userId)).thenReturn(user);

        LocalDate startDate = LocalDate.of(2026, 9, 1);

        // Chama o método real diretamente (sem assertThrows) — hoje FALHA com
        // UnsupportedOperationException antes de qualquer asserção abaixo.
        ItineraryResponseDTO result = service.adopt(templateId, userId, startDate);

        assertEquals(startDate, result.startDate());
        assertEquals(startDate.plusDays(1), result.endDate()); // durationDays=2 → plusDays(2-1)
        assertEquals(destinationId, result.destinationId());
        assertEquals("Lisboa", result.destinationName());
        assertEquals(2, result.days().size());

        assertEquals(1, result.days().get(0).dayNumber());
        assertEquals(startDate, result.days().get(0).date());
        assertEquals(ItinerarySlot.MORNING, result.days().get(0).activities().get(0).slot());
        assertEquals(poi1.getId(), result.days().get(0).activities().get(0).poi().id());

        assertEquals(2, result.days().get(1).dayNumber());
        assertEquals(startDate.plusDays(1), result.days().get(1).date());
        assertEquals(ItinerarySlot.AFTERNOON, result.days().get(1).activities().get(0).slot());
        assertEquals(poi2.getId(), result.days().get(1).activities().get(0).poi().id());
    }

    // ─── (b) adopt — template inexistente → ResourceNotFoundException ─────────

    @Test
    void adopt_shouldThrowResourceNotFound_whenTemplateDoesNotExist() {
        when(templateRepo.findById(templateId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> service.adopt(templateId, userId, LocalDate.now()));
    }

    // ─── (c) generateForDestination — destino inexistente → ResourceNotFoundException ─

    @Test
    void generateForDestination_shouldThrowResourceNotFound_whenDestinationSlugDoesNotExist() {
        when(destinationRepo.findBySlug("unknown")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> service.generateForDestination("unknown", 3, BudgetTier.MODERATE));
    }

    // ─── (d) generateForDestination — poiId alucinado pelo LLM é descartado silenciosamente ─
    //
    // Contrato assumido (a confirmar/ajustar pelo próximo agente): o service
    // busca os POIs ativos reais do destino (poiRepo.findAllByDestinationIdAndActiveTrue),
    // monta um prompt listando {id, title, category, budgetTier} de cada um,
    // chama chatClient.prompt().user(prompt).call().entity(GeneratedTemplateContent.class),
    // e para cada GeneratedTemplateActivity da resposta valida se o poiId
    // pertence ao conjunto de POIs reais buscado — se não pertencer (poiId
    // alucinado/inventado), a atividade é APENAS ignorada (log + skip), sem
    // falhar a requisição inteira. Este mock documenta esse contrato; hoje é
    // inerte porque o stub lança antes de tocar no ChatClient.

    @Test
    @SuppressWarnings("unchecked")
    void generateForDestination_shouldSkipHallucinatedPoiId_andKeepOnlyRealOnes() {
        Destination destination = Destination.builder().id(destinationId).slug("lisboa").name("Lisboa").build();
        when(destinationRepo.findBySlug("lisboa")).thenReturn(Optional.of(destination));

        UUID realPoiId = UUID.randomUUID();
        Poi realPoi = Poi.builder().id(realPoiId).destination(destination).title("Torre de Belém")
            .category(PoiCategory.MUSEUM).budgetTier(BudgetTier.MODERATE).build();
        when(poiRepo.findAllByDestinationIdAndActiveTrue(destinationId)).thenReturn(List.of(realPoi));

        // Resposta "do LLM": uma atividade com poiId real, outra com poiId
        // que NÃO existe no catálogo do destino (alucinação).
        GeneratedTemplateActivity validActivity = new GeneratedTemplateActivity(realPoiId.toString(), 1, "MORNING", 0);
        GeneratedTemplateActivity hallucinatedActivity = new GeneratedTemplateActivity(UUID.randomUUID().toString(), 1, "AFTERNOON", 0);
        GeneratedTemplateContent llmResponse = new GeneratedTemplateContent(
            "Lisboa Clássica", "1 dia de história e cultura",
            List.of(validActivity, hallucinatedActivity)
        );

        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.entity(GeneratedTemplateContent.class)).thenReturn(llmResponse);

        ItineraryTemplateDTO result = service.generateForDestination("lisboa", 1, BudgetTier.MODERATE);

        // O template deve ter sido criado normalmente (a requisição não
        // falha por causa do único poiId alucinado).
        assertNotNull(result);
        assertEquals(destinationId, result.destinationId());
        assertEquals(BudgetTier.MODERATE, result.budgetTier());
    }

    // ─── (d.2) generateForDestination — poiId com o TÍTULO do POI em vez do UUID
    // é recuperado por casamento de título, não descartado ─────────────────────
    //
    // Achado em validação manual ao vivo (LLM real, catálogo real): a LLM às
    // vezes devolve o CAMPO ERRADO — o título do POI em vez do UUID pedido
    // explicitamente no prompt (ex.: "9/11 Memorial & Museum" em vez do seu
    // id). Antes desta correção, isso era tratado como um poiId
    // alucinado/inválido e a atividade era descartada — deixando um período
    // (MORNING/AFTERNOON/EVENING) vazio no roteiro pronto entregue ao
    // usuário. Contrato desta correção: quando poiId não é um UUID
    // sintaticamente válido, tenta casar o valor recebido contra o TÍTULO
    // real de algum POI do catálogo do destino (comparação normalizada:
    // trim + lower-case) antes de desistir e descartar a atividade.
    @Test
    @SuppressWarnings("unchecked")
    void generateForDestination_shouldRecoverActivity_whenLlmReturnsPoiTitleInsteadOfUuid() {
        Destination destination = Destination.builder().id(destinationId).slug("nova-york").name("Nova York").build();
        when(destinationRepo.findBySlug("nova-york")).thenReturn(Optional.of(destination));

        UUID realPoiId = UUID.randomUUID();
        Poi realPoi = Poi.builder().id(realPoiId).destination(destination).title("9/11 Memorial & Museum")
            .category(PoiCategory.MUSEUM).budgetTier(BudgetTier.MODERATE).build();
        when(poiRepo.findAllByDestinationIdAndActiveTrue(destinationId)).thenReturn(List.of(realPoi));

        GeneratedTemplateActivity activityWithTitleInsteadOfId =
            new GeneratedTemplateActivity("9/11 Memorial & Museum", 1, "AFTERNOON", 0);
        GeneratedTemplateContent llmResponse = new GeneratedTemplateContent(
            "Nova York Clássica", "1 dia de história", List.of(activityWithTitleInsteadOfId)
        );

        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.entity(GeneratedTemplateContent.class)).thenReturn(llmResponse);

        ArgumentCaptor<ItineraryTemplate> templateCaptor = ArgumentCaptor.forClass(ItineraryTemplate.class);

        service.generateForDestination("nova-york", 1, BudgetTier.MODERATE);

        verify(templateRepo).save(templateCaptor.capture());
        ItineraryTemplate saved = templateCaptor.getValue();

        assertEquals(1, saved.getDays().size(),
            "A atividade recuperada por título deveria ter sido persistida (1 dia com 1 atividade), não descartada");
        assertEquals(1, saved.getDays().get(0).getActivities().size());
        assertEquals(realPoiId, saved.getDays().get(0).getActivities().get(0).getPoi().getId());
    }

    // ─── (d.3) generateForDestination — o casamento por título é normalizado
    // (trim + case-insensitive), não uma igualdade exata de string ──────────
    @Test
    @SuppressWarnings("unchecked")
    void generateForDestination_shouldMatchPoiTitleCaseInsensitively_andIgnoringSurroundingWhitespace() {
        Destination destination = Destination.builder().id(destinationId).slug("nova-york").name("Nova York").build();
        when(destinationRepo.findBySlug("nova-york")).thenReturn(Optional.of(destination));

        UUID realPoiId = UUID.randomUUID();
        Poi realPoi = Poi.builder().id(realPoiId).destination(destination).title("Empire State Building")
            .category(PoiCategory.LANDMARK).budgetTier(BudgetTier.MODERATE).build();
        when(poiRepo.findAllByDestinationIdAndActiveTrue(destinationId)).thenReturn(List.of(realPoi));

        GeneratedTemplateActivity activityWithDifferentCasingAndSpacing =
            new GeneratedTemplateActivity("  empire state building  ", 1, "MORNING", 0);
        GeneratedTemplateContent llmResponse = new GeneratedTemplateContent(
            "Nova York Clássica", "1 dia de história", List.of(activityWithDifferentCasingAndSpacing)
        );

        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.entity(GeneratedTemplateContent.class)).thenReturn(llmResponse);

        ArgumentCaptor<ItineraryTemplate> templateCaptor = ArgumentCaptor.forClass(ItineraryTemplate.class);

        service.generateForDestination("nova-york", 1, BudgetTier.MODERATE);

        verify(templateRepo).save(templateCaptor.capture());
        ItineraryTemplate saved = templateCaptor.getValue();

        assertEquals(1, saved.getDays().size());
        assertEquals(realPoiId, saved.getDays().get(0).getActivities().get(0).getPoi().getId());
    }

    // ─── (e) generateForDestination — o prompt deve destacar POIs mustSee ────
    //
    // Contrato assumido (RED por design — a implementar por um próximo
    // agente em ItineraryTemplateService#buildGenerationPrompt): hoje cada
    // linha do catálogo de POIs no prompt segue o formato fixo
    // "- id: %s | title: %s | category: %s | budgetTier: %s" (ver
    // buildGenerationPrompt). Não é uma marcação "à escolha do agente" —
    // deve ser exatamente esse mesmo formato estendido com mais um campo
    // pipe-separado, "mustSee: %s" (true/false), pelo mesmo padrão dos
    // campos já existentes na linha. Este teste exige literalmente
    // "mustSee: true" na linha do POI imperdível e "mustSee: false" na
    // linha do POI comum — não aceita variações textuais.
    @Test
    @SuppressWarnings("unchecked")
    void generateForDestination_shouldHighlightMustSeePois_differentlyFromOthers_inPrompt() {
        Destination destination = Destination.builder().id(destinationId).slug("copenhague").name("Copenhague").build();
        when(destinationRepo.findBySlug("copenhague")).thenReturn(Optional.of(destination));

        Poi mustSeePoi = Poi.builder().id(UUID.randomUUID()).destination(destination)
            .title("A Pequena Sereia").category(PoiCategory.LANDMARK).budgetTier(BudgetTier.ECONOMY)
            .mustSee(true).build();
        Poi regularPoi = Poi.builder().id(UUID.randomUUID()).destination(destination)
            .title("Museu Nacional da Dinamarca").category(PoiCategory.MUSEUM).budgetTier(BudgetTier.MODERATE)
            .mustSee(false).build();
        when(poiRepo.findAllByDestinationIdAndActiveTrue(destinationId)).thenReturn(List.of(mustSeePoi, regularPoi));

        GeneratedTemplateContent llmResponse = new GeneratedTemplateContent(
            "Copenhague Essencial", "1 dia pelos imperdíveis de Copenhague", List.of()
        );

        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(promptCaptor.capture())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.entity(GeneratedTemplateContent.class)).thenReturn(llmResponse);

        service.generateForDestination("copenhague", 1, BudgetTier.MODERATE);

        String prompt = promptCaptor.getValue();

        String mustSeeLine = extractLineFor(prompt, "A Pequena Sereia");
        String regularLine = extractLineFor(prompt, "Museu Nacional da Dinamarca");

        assertNotNull(mustSeeLine, "O POI mustSee=true deve aparecer no catálogo do prompt");
        assertNotNull(regularLine, "O POI mustSee=false deve aparecer no catálogo do prompt");

        assertTrue(mustSeeLine.contains("mustSee: true"),
            "A linha do catálogo do POI imperdível deve conter exatamente \"mustSee: true\", " +
            "seguindo o mesmo formato pipe-separado dos outros campos (id/title/category/budgetTier)");
        assertTrue(regularLine.contains("mustSee: false"),
            "A linha do catálogo do POI comum deve conter exatamente \"mustSee: false\"");
    }

    private String extractLineFor(String prompt, String poiTitle) {
        return prompt.lines()
            .filter(line -> line.contains(poiTitle))
            .findFirst()
            .orElse(null);
    }

    // ─── (f) generateForDestination — o prompt deve mandar o LLM se basear em
    // roteiros/guias de viagem REAIS e publicados para o destino específico,
    // não apenas "agir como se" um roteiro real fosse montado de forma
    // abstrata/genérica ─────────────────────────────────────────────────────
    //
    // Contrato assumido (RED por design — a implementar por um próximo agente
    // em ItineraryTemplateService#buildGenerationPrompt): o prompt atual já
    // instrui o LLM a montar a sequência do dia "como um roteiro real e
    // conhecido daquela cidade", mas isso é uma instrução abstrata — não
    // referencia explicitamente guias/roteiros de viagem REAIS e PUBLICADOS
    // sobre o destino nomeado. A frase exata exigida por este teste é
    // "guias de viagem reais e publicados" (a implementação deve conter essa
    // string literal, em algum lugar do prompt, parametrizada pelo destino —
    // não é uma marcação "à escolha do agente", já que um agente futuro
    // precisa casar exatamente com essa string). Além disso o teste confirma
    // que o prompt não é boilerplate genérico: o nome do destino
    // (destination.getName()) deve aparecer interpolado no texto do prompt.
    @Test
    @SuppressWarnings("unchecked")
    void generateForDestination_shouldInstructLlmToBaseItineraryOnRealPublishedTravelGuides_forTheNamedDestination() {
        Destination destination = Destination.builder().id(destinationId).slug("kyoto").name("Kyoto").build();
        when(destinationRepo.findBySlug("kyoto")).thenReturn(Optional.of(destination));

        Poi poi = Poi.builder().id(UUID.randomUUID()).destination(destination)
            .title("Fushimi Inari Taisha").category(PoiCategory.LANDMARK).budgetTier(BudgetTier.ECONOMY)
            .mustSee(true).build();
        when(poiRepo.findAllByDestinationIdAndActiveTrue(destinationId)).thenReturn(List.of(poi));

        GeneratedTemplateContent llmResponse = new GeneratedTemplateContent(
            "Kyoto Essencial", "1 dia pelos imperdíveis de Kyoto", List.of()
        );

        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(promptCaptor.capture())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.entity(GeneratedTemplateContent.class)).thenReturn(llmResponse);

        service.generateForDestination("kyoto", 1, BudgetTier.MODERATE);

        String prompt = promptCaptor.getValue();

        assertTrue(prompt.contains("guias de viagem reais e publicados"),
            "O prompt deve instruir explicitamente o LLM a se basear em guias de viagem reais e " +
            "publicados sobre o destino (frase exata exigida: \"guias de viagem reais e publicados\"), " +
            "e não apenas dizer para \"agir como se\" fosse um roteiro real de forma abstrata");
        assertTrue(prompt.contains(destination.getName()),
            "O prompt deve interpolar o nome real do destino (destination.getName()), provando que a " +
            "instrução é parametrizada por destino e não um boilerplate genérico");
    }

    // ─── (g) generateForDestination — o catálogo do prompt é dividido em seções
    // por dia, agrupando POIs por proximidade geográfica real (lat/lng), não
    // só por instrução de texto ──────────────────────────────────────────────
    //
    // Contrato (clusterPoisByDay): k-means determinístico sobre lat/lng, com
    // sementes escolhidas por ordenação de latitude — o grupo de latitude mais
    // baixa sempre vira a seção "Dia 1". Achado real que motivou isso:
    // comparação manual contra um roteiro de referência da contratante mostrou
    // a LLM escolhendo POIs a ~10km de distância dos outros do mesmo dia,
    // mesmo com instrução explícita de proximidade só em texto — por isso o
    // agrupamento agora é estrutural (o catálogo já vem pré-filtrado por dia),
    // não apenas sugerido.
    @Test
    @SuppressWarnings("unchecked")
    void generateForDestination_shouldPartitionPoiCatalogByGeographicProximity_intoPerDaySections() {
        Destination destination = Destination.builder().id(destinationId).slug("paris").name("Paris").build();
        when(destinationRepo.findBySlug("paris")).thenReturn(Optional.of(destination));

        // Grupo A — região de latitude mais baixa, 2 POIs a ~150m um do outro.
        Poi poiA1 = Poi.builder().id(UUID.randomUUID()).destination(destination)
            .title("Torre Sul A").category(PoiCategory.LANDMARK).budgetTier(BudgetTier.MODERATE)
            .latitude(BigDecimal.valueOf(48.800)).longitude(BigDecimal.valueOf(2.300)).mustSee(true).build();
        Poi poiA2 = Poi.builder().id(UUID.randomUUID()).destination(destination)
            .title("Museu Sul A").category(PoiCategory.MUSEUM).budgetTier(BudgetTier.MODERATE)
            .latitude(BigDecimal.valueOf(48.801)).longitude(BigDecimal.valueOf(2.301)).mustSee(false).build();

        // Grupo B — região de latitude bem mais alta (~17km de distância do
        // grupo A), 2 POIs a ~150m um do outro.
        Poi poiB1 = Poi.builder().id(UUID.randomUUID()).destination(destination)
            .title("Torre Norte B").category(PoiCategory.LANDMARK).budgetTier(BudgetTier.MODERATE)
            .latitude(BigDecimal.valueOf(48.950)).longitude(BigDecimal.valueOf(2.500)).mustSee(true).build();
        Poi poiB2 = Poi.builder().id(UUID.randomUUID()).destination(destination)
            .title("Museu Norte B").category(PoiCategory.MUSEUM).budgetTier(BudgetTier.MODERATE)
            .latitude(BigDecimal.valueOf(48.951)).longitude(BigDecimal.valueOf(2.501)).mustSee(false).build();

        when(poiRepo.findAllByDestinationIdAndActiveTrue(destinationId))
            .thenReturn(List.of(poiA1, poiA2, poiB1, poiB2));

        GeneratedTemplateContent llmResponse = new GeneratedTemplateContent("Paris 2 Dias", "desc", List.of());

        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(promptCaptor.capture())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.entity(GeneratedTemplateContent.class)).thenReturn(llmResponse);

        service.generateForDestination("paris", 2, BudgetTier.MODERATE);

        String prompt = promptCaptor.getValue();
        String day1Section = extractSection(prompt, "Dia 1", "Dia 2");
        String day2Section = extractSection(prompt, "Dia 2", null);

        assertTrue(day1Section.contains("Torre Sul A") && day1Section.contains("Museu Sul A"),
            "A seção \"Dia 1\" do catálogo deve conter os dois POIs do grupo geográfico de latitude mais baixa");
        assertFalse(day1Section.contains("Torre Norte B") || day1Section.contains("Museu Norte B"),
            "A seção \"Dia 1\" não deve conter POIs do outro grupo geográfico (~17km de distância)");

        assertTrue(day2Section.contains("Torre Norte B") && day2Section.contains("Museu Norte B"),
            "A seção \"Dia 2\" do catálogo deve conter os dois POIs do grupo geográfico de latitude mais alta");
        assertFalse(day2Section.contains("Torre Sul A") || day2Section.contains("Museu Sul A"),
            "A seção \"Dia 2\" não deve conter POIs do outro grupo geográfico");
    }

    // ─── (h) generateForDestination — o dayNumber devolvido pela LLM é
    // SOBRESCRITO pelo cluster geográfico real, nunca aceito cegamente ───────
    //
    // Mesmo cenário geográfico do teste (g), mas aqui a LLM "mente": devolve
    // um poiId do grupo B (latitude alta, cluster/"Dia 2") marcado com
    // dayNumber: 1. O template salvo deve corrigir isso e colocar a atividade
    // no dia 2 de verdade — a divisão geográfica é a fonte de verdade, não o
    // campo dayNumber que a LLM escreveu.
    @Test
    @SuppressWarnings("unchecked")
    void generateForDestination_shouldOverrideLlmDayNumber_withRealGeographicCluster() {
        Destination destination = Destination.builder().id(destinationId).slug("paris").name("Paris").build();
        when(destinationRepo.findBySlug("paris")).thenReturn(Optional.of(destination));

        Poi poiA1 = Poi.builder().id(UUID.randomUUID()).destination(destination)
            .title("Torre Sul A").category(PoiCategory.LANDMARK).budgetTier(BudgetTier.MODERATE)
            .latitude(BigDecimal.valueOf(48.800)).longitude(BigDecimal.valueOf(2.300)).mustSee(true).build();
        Poi poiB1 = Poi.builder().id(UUID.randomUUID()).destination(destination)
            .title("Torre Norte B").category(PoiCategory.LANDMARK).budgetTier(BudgetTier.MODERATE)
            .latitude(BigDecimal.valueOf(48.950)).longitude(BigDecimal.valueOf(2.500)).mustSee(true).build();

        when(poiRepo.findAllByDestinationIdAndActiveTrue(destinationId)).thenReturn(List.of(poiA1, poiB1));

        // LLM devolve o POI do grupo B (geograficamente "Dia 2") marcado
        // erroneamente como dayNumber: 1 — deliberado, é exatamente o tipo de
        // erro observado na comparação manual que motivou essa mudança.
        GeneratedTemplateActivity misclassified = new GeneratedTemplateActivity(poiB1.getId().toString(), 1, "MORNING", 0);
        GeneratedTemplateContent llmResponse = new GeneratedTemplateContent(
            "Paris 2 Dias", "desc", List.of(misclassified)
        );

        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.entity(GeneratedTemplateContent.class)).thenReturn(llmResponse);

        ArgumentCaptor<ItineraryTemplate> templateCaptor = ArgumentCaptor.forClass(ItineraryTemplate.class);

        service.generateForDestination("paris", 2, BudgetTier.MODERATE);

        verify(templateRepo).save(templateCaptor.capture());
        ItineraryTemplate saved = templateCaptor.getValue();

        boolean poiBFoundInDay2 = saved.getDays().stream()
            .filter(d -> d.getDayNumber() == 2)
            .flatMap(d -> d.getActivities().stream())
            .anyMatch(a -> a.getPoi().getId().equals(poiB1.getId()));
        boolean poiBFoundInDay1 = saved.getDays().stream()
            .filter(d -> d.getDayNumber() == 1)
            .flatMap(d -> d.getActivities().stream())
            .anyMatch(a -> a.getPoi().getId().equals(poiB1.getId()));

        assertTrue(poiBFoundInDay2,
            "O POI do grupo geográfico de latitude alta deve acabar no dia 2 (cluster real), mesmo a " +
            "LLM tendo devolvido dayNumber: 1 pra ele");
        assertFalse(poiBFoundInDay1,
            "O POI não deve ficar no dia 1 só porque a LLM devolveu esse dayNumber — o cluster geográfico " +
            "prevalece");
    }

    private String extractSection(String prompt, String startMarker, String endMarker) {
        int start = prompt.indexOf(startMarker);
        int end = endMarker == null ? prompt.indexOf("\n\n", start) : prompt.indexOf(endMarker, start);
        if (end == -1) end = prompt.length();
        return prompt.substring(start, end);
    }
}
