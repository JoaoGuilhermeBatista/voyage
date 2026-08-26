package luvoygroup.voyage.features.admin;

import luvoygroup.voyage.domain.Destination;
import luvoygroup.voyage.domain.Poi;
import luvoygroup.voyage.domain.enums.BudgetTier;
import luvoygroup.voyage.domain.enums.PoiCategory;
import luvoygroup.voyage.features.admin.dto.CatholicChurchClassification;
import luvoygroup.voyage.features.admin.dto.GeneratedPoi;
import luvoygroup.voyage.features.admin.dto.PoiEnrichmentReport;
import luvoygroup.voyage.features.admin.dto.PoisWrapper;
import luvoygroup.voyage.features.itinerarytemplates.ItineraryTemplateService;
import luvoygroup.voyage.repository.DestinationRepository;
import luvoygroup.voyage.repository.PoiRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * Testes unitários "standalone" de AdminService — mesmo padrão estabelecido
 * por ItineraryTemplateServiceTest: NÃO usa @ExtendWith(MockitoExtension.class)
 * de propósito (mocks via Mockito.mock() direto), o service é instanciado
 * diretamente via seu construtor gerado pelo Lombok (@RequiredArgsConstructor).
 *
 * Estes dois primeiros testes cobrem a REGRESSÃO real relatada: antes da
 * adição da categoria LANDMARK ao enum PoiCategory e do campo mustSee em
 * Poi/GeneratedPoi, um GeneratedPoi com category="LANDMARK" fazia
 * PoiCategory.valueOf(...) lançar IllegalArgumentException, caindo no catch
 * genérico de AdminService#mapToPoi e sendo descartado silenciosamente (log
 * de warn) — exatamente o bug que descartou "A Pequena Sereia", "Palácio de
 * Amalienborg" e "Copenhagen Opera House" em Copenhague. Com o enum e o
 * mapeamento já corrigidos (mudança mecânica feita junto com estes testes),
 * estes dois testes já devem passar em VERDE.
 */
class AdminServiceTest {

    private LlmService llmService;
    private DestinationRepository destinationRepo;
    private PoiRepository poiRepo;
    private ItineraryTemplateService itineraryTemplateService;

    private AdminService service;

    private Destination destination;

    @BeforeEach
    void setUp() {
        llmService = mock(LlmService.class);
        destinationRepo = mock(DestinationRepository.class);
        poiRepo = mock(PoiRepository.class);
        itineraryTemplateService = mock(ItineraryTemplateService.class);

        service = new AdminService(llmService, destinationRepo, poiRepo, itineraryTemplateService);

        destination = Destination.builder()
            .id(UUID.randomUUID())
            .slug("copenhague")
            .name("Copenhague")
            .city("Copenhagen")
            .country("Dinamarca")
            .build();
    }

    // ─── Regressão: LANDMARK + mustSee=true não é mais descartado ────────────

    @Test
    void generateAndSavePoisForDestination_shouldSaveLandmarkPoi_asMustSee_insteadOfDiscardingIt() {
        GeneratedPoi landmark = new GeneratedPoi(
            "A Pequena Sereia",
            "LANDMARK",
            "Estátua de bronze inspirada no conto de Hans Christian Andersen",
            "Langelinie, Copenhagen",
            55.6928,
            12.5993,
            "ECONOMY",
            30,
            null,
            null,
            List.of("historia", "cultura"),
            true,
            false
        );

        when(llmService.generatePois("Copenhagen", "Dinamarca"))
            .thenReturn(new PoisWrapper(List.of(landmark)));

        when(poiRepo.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        int count = service.generateAndSavePoisForDestination(destination);

        assertEquals(1, count);

        ArgumentCaptor<List<Poi>> captor = ArgumentCaptor.forClass(List.class);
        verify(poiRepo).saveAll(captor.capture());

        List<Poi> saved = captor.getValue();
        assertEquals(1, saved.size());
        Poi savedPoi = saved.get(0);
        assertEquals(PoiCategory.LANDMARK, savedPoi.getCategory());
        assertTrue(savedPoi.isMustSee());
        assertEquals("A Pequena Sereia", savedPoi.getTitle());
    }

    // ─── Regressão: mustSee=null não lança NPE e vira false ──────────────────

    @Test
    void generateAndSavePoisForDestination_shouldMapNullMustSee_toFalse_withoutThrowing() {
        GeneratedPoi withoutMustSeeFlag = new GeneratedPoi(
            "Museu Nacional da Dinamarca",
            "MUSEUM",
            "Maior museu de história cultural do país",
            "Ny Vestergade 10, Copenhagen",
            55.6737,
            12.5730,
            "MODERATE",
            120,
            "10:00",
            "17:00",
            List.of("cultura"),
            null,
            null
        );

        when(llmService.generatePois("Copenhagen", "Dinamarca"))
            .thenReturn(new PoisWrapper(List.of(withoutMustSeeFlag)));

        when(poiRepo.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> {
            int count = service.generateAndSavePoisForDestination(destination);
            assertEquals(1, count);
        });

        ArgumentCaptor<List<Poi>> captor = ArgumentCaptor.forClass(List.class);
        verify(poiRepo).saveAll(captor.capture());

        Poi savedPoi = captor.getValue().get(0);
        assertFalse(savedPoi.isMustSee());
        assertFalse(savedPoi.isCatholicChurch());
    }

    // ─── catholicChurch=true no GeneratedPoi é propagado para o Poi salvo ────

    @Test
    void generateAndSavePoisForDestination_shouldMapCatholicChurchTrue_fromGeneratedPoi() {
        GeneratedPoi cathedral = new GeneratedPoi(
            "Catedral Nossa Senhora de Copenhague",
            "LANDMARK",
            "Catedral católica no centro de Copenhague",
            "Nørregade 8, Copenhagen",
            55.6779,
            12.5719,
            "ECONOMY",
            45,
            "08:00",
            "18:00",
            List.of("religiao", "historia"),
            false,
            true
        );

        when(llmService.generatePois("Copenhagen", "Dinamarca"))
            .thenReturn(new PoisWrapper(List.of(cathedral)));

        when(poiRepo.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        int count = service.generateAndSavePoisForDestination(destination);

        assertEquals(1, count);

        ArgumentCaptor<List<Poi>> captor = ArgumentCaptor.forClass(List.class);
        verify(poiRepo).saveAll(captor.capture());

        Poi savedPoi = captor.getValue().get(0);
        assertTrue(savedPoi.isCatholicChurch());
    }

    // ─── enrichExistingPoisWithCatholicChurchFlag — contrato completo ────────
    //
    // Contrato assumido (RED por design — ver contrato completo documentado
    // como comentário acima de AdminService#enrichExistingPoisWithCatholicChurchFlag,
    // a implementar por um próximo agente): para cada destino ativo com pelo
    // menos 1 POI ativo, busca os títulos desses POIs reais, chama
    // llmService.classifyCatholicChurches(...), e SÓ marca
    // catholicChurch=true nos POIs cujo título bate EXATAMENTE (verbatim)
    // com um título real do destino — qualquer título "alucinado" retornado
    // pelo LLM que não exista no catálogo real daquele destino é
    // silenciosamente ignorado (mesmo guard-rail de hallucination-skip de
    // ItineraryTemplateService#validateGeneratedActivity). POIs nunca são
    // deletados/recriados — apenas atualizados in place via poiRepo.save.
    //
    // Fixture: 2 destinos (Lisboa, Roma), cada um com 1 POI que É realmente
    // uma igreja católica e 1 POI que não é. O mock de classifyCatholicChurches
    // devolve, por destino, o título real da igreja MAIS um título
    // inventado que não corresponde a nenhum POI real daquele destino.
    @Test
    void enrichExistingPoisWithCatholicChurchFlag_shouldFlagOnlyRealMatchingTitles_andIgnoreHallucinations() {
        Destination lisboa = Destination.builder()
            .id(UUID.randomUUID()).slug("lisboa").name("Lisboa").city("Lisboa").country("Portugal")
            .active(true).build();
        Destination roma = Destination.builder()
            .id(UUID.randomUUID()).slug("roma").name("Roma").city("Roma").country("Itália")
            .active(true).build();

        Poi lisboaCathedral = Poi.builder().id(UUID.randomUUID()).destination(lisboa)
            .title("Sé de Lisboa").category(PoiCategory.LANDMARK).budgetTier(BudgetTier.ECONOMY)
            .active(true).catholicChurch(false).build();
        Poi lisboaMuseum = Poi.builder().id(UUID.randomUUID()).destination(lisboa)
            .title("Museu Nacional de Arte Antiga").category(PoiCategory.MUSEUM).budgetTier(BudgetTier.MODERATE)
            .active(true).catholicChurch(false).build();

        Poi romaBasilica = Poi.builder().id(UUID.randomUUID()).destination(roma)
            .title("Basílica de São Pedro").category(PoiCategory.LANDMARK).budgetTier(BudgetTier.ECONOMY)
            .active(true).catholicChurch(false).build();
        Poi romaRestaurant = Poi.builder().id(UUID.randomUUID()).destination(roma)
            .title("Trattoria da Enzo").category(PoiCategory.RESTAURANT).budgetTier(BudgetTier.MODERATE)
            .active(true).catholicChurch(false).build();

        when(destinationRepo.findAllByActiveTrue()).thenReturn(List.of(lisboa, roma));
        when(poiRepo.findAllByDestinationIdAndActiveTrue(lisboa.getId()))
            .thenReturn(List.of(lisboaCathedral, lisboaMuseum));
        when(poiRepo.findAllByDestinationIdAndActiveTrue(roma.getId()))
            .thenReturn(List.of(romaBasilica, romaRestaurant));

        // Um título real por destino + um título alucinado (não existe no catálogo real do destino).
        when(llmService.classifyCatholicChurches(eq("Lisboa"), eq("Portugal"), anyList()))
            .thenReturn(new CatholicChurchClassification(List.of("Sé de Lisboa", "Catedral Inventada Pelo LLM")));
        when(llmService.classifyCatholicChurches(eq("Roma"), eq("Itália"), anyList()))
            .thenReturn(new CatholicChurchClassification(List.of("Basílica de São Pedro", "Igreja Que Não Existe")));

        when(poiRepo.save(any(Poi.class))).thenAnswer(inv -> inv.getArgument(0));

        // Chama o método real diretamente (sem assertThrows) — hoje FALHA com
        // UnsupportedOperationException antes de qualquer asserção abaixo.
        PoiEnrichmentReport report = service.enrichExistingPoisWithCatholicChurchFlag();

        verify(poiRepo).save(argThat(p -> p.getId().equals(lisboaCathedral.getId()) && p.isCatholicChurch()));
        verify(poiRepo).save(argThat(p -> p.getId().equals(romaBasilica.getId()) && p.isCatholicChurch()));
        verify(poiRepo, never()).save(argThat(p -> p != null && p.getId().equals(lisboaMuseum.getId())));
        verify(poiRepo, never()).save(argThat(p -> p != null && p.getId().equals(romaRestaurant.getId())));

        assertEquals(2, report.destinationsProcessed());
        assertEquals(2, report.totalPoisEnriched());
        assertTrue(report.processedSlugs().containsAll(List.of("lisboa", "roma")));
    }
}
