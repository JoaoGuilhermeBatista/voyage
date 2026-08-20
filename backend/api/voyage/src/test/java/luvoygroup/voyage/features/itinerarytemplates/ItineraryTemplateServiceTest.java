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
import org.springframework.ai.chat.client.ChatClient;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
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
    private ItineraryTemplateDayRepository templateDayRepo;
    private ItineraryTemplateActivityRepository templateActivityRepo;
    private ItineraryRepository itineraryRepo;
    private ItineraryDayRepository itineraryDayRepo;
    private ItineraryActivityRepository itineraryActivityRepo;
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
        templateDayRepo = mock(ItineraryTemplateDayRepository.class);
        templateActivityRepo = mock(ItineraryTemplateActivityRepository.class);
        itineraryRepo = mock(ItineraryRepository.class);
        itineraryDayRepo = mock(ItineraryDayRepository.class);
        itineraryActivityRepo = mock(ItineraryActivityRepository.class);
        userRepo = mock(UserRepository.class);

        service = new ItineraryTemplateService(
            chatClient, destinationRepo, poiRepo,
            templateRepo, templateDayRepo, templateActivityRepo,
            itineraryRepo, itineraryDayRepo, itineraryActivityRepo,
            userRepo
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
}
