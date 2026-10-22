package luvoygroup.voyage.features.admin;

import luvoygroup.voyage.domain.Destination;
import luvoygroup.voyage.domain.Poi;
import luvoygroup.voyage.domain.enums.BudgetTier;
import luvoygroup.voyage.domain.enums.PoiCategory;
import luvoygroup.voyage.features.admin.dto.CatholicChurchClassification;
import luvoygroup.voyage.features.admin.dto.GeneratedPoi;
import luvoygroup.voyage.features.admin.dto.PoiEnrichmentClassification;
import luvoygroup.voyage.features.admin.dto.PoiEnrichmentReport;
import luvoygroup.voyage.features.admin.dto.PoiTitleChange;
import luvoygroup.voyage.features.admin.dto.PoiTitleNormalization;
import luvoygroup.voyage.features.admin.dto.PoisWrapper;
import luvoygroup.voyage.features.itinerarytemplates.ItineraryTemplateService;
import luvoygroup.voyage.features.unusualplaces.UnusualPlaceService;
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
    private UnusualPlaceService unusualPlaceService;
    private luvoygroup.voyage.features.maps.PlacesService placesService;
    private luvoygroup.voyage.features.maps.WikimediaService wikimediaService;

    private AdminService service;

    private Destination destination;

    @BeforeEach
    void setUp() {
        llmService = mock(LlmService.class);
        destinationRepo = mock(DestinationRepository.class);
        poiRepo = mock(PoiRepository.class);
        itineraryTemplateService = mock(ItineraryTemplateService.class);
        unusualPlaceService = mock(UnusualPlaceService.class);
        placesService = mock(luvoygroup.voyage.features.maps.PlacesService.class);
        wikimediaService = mock(luvoygroup.voyage.features.maps.WikimediaService.class);

        service = new AdminService(llmService, destinationRepo, poiRepo, itineraryTemplateService, unusualPlaceService, placesService, wikimediaService);

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
            false,
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
            true,
            false
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

    // ─── enrichExistingPoisWithMustSeeAndLandmark — contrato completo (STUB, RED) ─
    //
    // AdminService#enrichExistingPoisWithMustSeeAndLandmark é hoje um STUB que
    // lança UnsupportedOperationException (ver javadoc do método para o
    // contrato completo que um próximo agente vai implementar, após aprovação
    // humana). Os testes abaixo documentam o comportamento ESPERADO desse
    // contrato e, por isso, devem FALHAR agora (RED) com
    // UnsupportedOperationException em vez das asserções de negócio — não é
    // erro de compilação, é o RED intencional do TDD.

    @Test
    void enrichExistingPoisWithMustSeeAndLandmark_shouldClassifyAndApplyInPlace_forDestinationWithNoMustSeePoi() {
        Destination roma = Destination.builder()
            .id(UUID.randomUUID()).slug("roma").name("Roma").city("Roma").country("Itália")
            .active(true).build();

        Poi coliseu = Poi.builder().id(UUID.randomUUID()).destination(roma)
            .title("Coliseu").category(PoiCategory.MUSEUM).budgetTier(BudgetTier.MODERATE)
            .active(true).mustSee(false).build();
        Poi fontanaDiTrevi = Poi.builder().id(UUID.randomUUID()).destination(roma)
            .title("Fontana di Trevi").category(PoiCategory.PARK).budgetTier(BudgetTier.ECONOMY)
            .active(true).mustSee(false).build();
        Poi restaurante = Poi.builder().id(UUID.randomUUID()).destination(roma)
            .title("Trattoria da Enzo").category(PoiCategory.RESTAURANT).budgetTier(BudgetTier.MODERATE)
            .active(true).mustSee(false).build();

        when(destinationRepo.findAllByActiveTrue()).thenReturn(List.of(roma));
        when(poiRepo.findAllByDestinationIdAndActiveTrue(roma.getId()))
            .thenReturn(List.of(coliseu, fontanaDiTrevi, restaurante));
        when(llmService.classifyMustSeeAndLandmarks(eq("Roma"), eq("Itália"), anyList()))
            .thenReturn(new PoiEnrichmentClassification(
                List.of("Coliseu", "Fontana di Trevi"),
                List.of("Coliseu", "Fontana di Trevi")
            ));
        when(poiRepo.save(any(Poi.class))).thenAnswer(inv -> inv.getArgument(0));

        // Hoje lança UnsupportedOperationException — RED esperado.
        PoiEnrichmentReport report = service.enrichExistingPoisWithMustSeeAndLandmark();

        verify(poiRepo).save(argThat(p -> p.getId().equals(coliseu.getId())
            && p.isMustSee() && p.getCategory() == PoiCategory.LANDMARK));
        verify(poiRepo).save(argThat(p -> p.getId().equals(fontanaDiTrevi.getId())
            && p.isMustSee() && p.getCategory() == PoiCategory.LANDMARK));
        verify(poiRepo, never()).save(argThat(p -> p != null && p.getId().equals(restaurante.getId())));

        assertEquals(1, report.destinationsProcessed());
        assertEquals(0, report.destinationsSkipped());
        assertEquals(2, report.totalPoisEnriched());
        assertTrue(report.processedSlugs().contains("roma"));
    }

    @Test
    void enrichExistingPoisWithMustSeeAndLandmark_shouldSkipDestination_thatAlreadyHasAMustSeePoi() {
        Destination paris = Destination.builder()
            .id(UUID.randomUUID()).slug("paris").name("Paris").city("Paris").country("França")
            .active(true).build();

        Poi torreEiffel = Poi.builder().id(UUID.randomUUID()).destination(paris)
            .title("Torre Eiffel").category(PoiCategory.LANDMARK).budgetTier(BudgetTier.MODERATE)
            .active(true).mustSee(true).build();

        when(destinationRepo.findAllByActiveTrue()).thenReturn(List.of(paris));
        when(poiRepo.findAllByDestinationIdAndActiveTrue(paris.getId()))
            .thenReturn(List.of(torreEiffel));

        // Hoje lança UnsupportedOperationException — RED esperado.
        PoiEnrichmentReport report = service.enrichExistingPoisWithMustSeeAndLandmark();

        verify(llmService, never()).classifyMustSeeAndLandmarks(anyString(), anyString(), anyList());
        verify(poiRepo, never()).save(any(Poi.class));

        assertEquals(0, report.destinationsProcessed());
        assertEquals(1, report.destinationsSkipped());
        assertFalse(report.processedSlugs().contains("paris"));
    }

    @Test
    void enrichExistingPoisWithMustSeeAndLandmark_shouldIgnoreHallucinatedTitle_withoutModifyingAnyPoi() {
        Destination madrid = Destination.builder()
            .id(UUID.randomUUID()).slug("madrid").name("Madrid").city("Madrid").country("Espanha")
            .active(true).build();

        Poi museuDoPrado = Poi.builder().id(UUID.randomUUID()).destination(madrid)
            .title("Museu do Prado").category(PoiCategory.MUSEUM).budgetTier(BudgetTier.MODERATE)
            .active(true).mustSee(false).build();

        when(destinationRepo.findAllByActiveTrue()).thenReturn(List.of(madrid));
        when(poiRepo.findAllByDestinationIdAndActiveTrue(madrid.getId()))
            .thenReturn(List.of(museuDoPrado));
        when(llmService.classifyMustSeeAndLandmarks(eq("Madrid"), eq("Espanha"), anyList()))
            .thenReturn(new PoiEnrichmentClassification(
                List.of("Monumento Inventado Pelo LLM"),
                List.of("Praça Que Não Existe")
            ));

        // Hoje lança UnsupportedOperationException — RED esperado.
        PoiEnrichmentReport report = service.enrichExistingPoisWithMustSeeAndLandmark();

        verify(poiRepo, never()).save(any(Poi.class));
        assertFalse(museuDoPrado.isMustSee());
        assertEquals(PoiCategory.MUSEUM, museuDoPrado.getCategory());
        assertEquals(0, report.totalPoisEnriched());
    }

    @Test
    void enrichExistingPoisWithMustSeeAndLandmark_shouldNotStop_whenOneDestinationFails() {
        Destination lisboa = Destination.builder()
            .id(UUID.randomUUID()).slug("lisboa").name("Lisboa").city("Lisboa").country("Portugal")
            .active(true).build();
        Destination madrid = Destination.builder()
            .id(UUID.randomUUID()).slug("madrid").name("Madrid").city("Madrid").country("Espanha")
            .active(true).build();

        Poi torreDeBelem = Poi.builder().id(UUID.randomUUID()).destination(lisboa)
            .title("Torre de Belém").category(PoiCategory.MUSEUM).budgetTier(BudgetTier.MODERATE)
            .active(true).mustSee(false).build();
        Poi puertaDelSol = Poi.builder().id(UUID.randomUUID()).destination(madrid)
            .title("Puerta del Sol").category(PoiCategory.PARK).budgetTier(BudgetTier.ECONOMY)
            .active(true).mustSee(false).build();

        when(destinationRepo.findAllByActiveTrue()).thenReturn(List.of(lisboa, madrid));
        when(poiRepo.findAllByDestinationIdAndActiveTrue(lisboa.getId()))
            .thenReturn(List.of(torreDeBelem));
        when(poiRepo.findAllByDestinationIdAndActiveTrue(madrid.getId()))
            .thenReturn(List.of(puertaDelSol));
        when(llmService.classifyMustSeeAndLandmarks(eq("Lisboa"), eq("Portugal"), anyList()))
            .thenThrow(new RuntimeException("Falha simulada do LLM"));
        when(llmService.classifyMustSeeAndLandmarks(eq("Madrid"), eq("Espanha"), anyList()))
            .thenReturn(new PoiEnrichmentClassification(List.of("Puerta del Sol"), List.of("Puerta del Sol")));
        when(poiRepo.save(any(Poi.class))).thenAnswer(inv -> inv.getArgument(0));

        // Hoje lança UnsupportedOperationException — RED esperado.
        PoiEnrichmentReport report = service.enrichExistingPoisWithMustSeeAndLandmark();

        verify(poiRepo).save(argThat(p -> p.getId().equals(puertaDelSol.getId())
            && p.isMustSee() && p.getCategory() == PoiCategory.LANDMARK));
        verify(poiRepo, never()).save(argThat(p -> p != null && p.getId().equals(torreDeBelem.getId())));

        assertEquals(1, report.destinationsProcessed());
        assertEquals(1, report.destinationsSkipped());
        assertTrue(report.processedSlugs().contains("madrid"));
        assertFalse(report.processedSlugs().contains("lisboa"));
    }

    // ─── enrichExistingPoisWithImages ─────────────────────────────────────────

    @Test
    void enrichExistingPoisWithImages_shouldFetchAndAppendPhoto_forEachPoiWithoutImages() {
        Destination roma = Destination.builder()
            .id(UUID.randomUUID()).slug("roma").name("Roma").city("Roma").country("Itália")
            .active(true).build();

        Poi coliseu = Poi.builder().id(UUID.randomUUID()).destination(roma)
            .title("Coliseu").category(PoiCategory.LANDMARK).budgetTier(BudgetTier.MODERATE)
            .active(true).build();
        Poi restaurante = Poi.builder().id(UUID.randomUUID()).destination(roma)
            .title("Trattoria da Enzo").category(PoiCategory.RESTAURANT).budgetTier(BudgetTier.MODERATE)
            .active(true).build();

        when(destinationRepo.findAllByActiveTrue()).thenReturn(List.of(roma));
        when(poiRepo.findAllByDestinationIdAndActiveTrue(roma.getId()))
            .thenReturn(List.of(coliseu, restaurante));
        when(placesService.fetchPhotoUrl("Coliseu", "Roma"))
            .thenReturn(java.util.Optional.of("https://lh3.googleusercontent.com/coliseu"));
        when(placesService.fetchPhotoUrl("Trattoria da Enzo", "Roma"))
            .thenReturn(java.util.Optional.of("https://lh3.googleusercontent.com/trattoria"));
        when(poiRepo.save(any(Poi.class))).thenAnswer(inv -> inv.getArgument(0));

        PoiEnrichmentReport report = service.enrichExistingPoisWithImages();

        assertTrue(coliseu.getImages().contains("https://lh3.googleusercontent.com/coliseu"));
        assertTrue(restaurante.getImages().contains("https://lh3.googleusercontent.com/trattoria"));
        assertEquals(1, report.destinationsProcessed());
        assertEquals(0, report.destinationsSkipped());
        assertEquals(2, report.totalPoisEnriched());
    }

    @Test
    void enrichExistingPoisWithImages_shouldSkipDestination_whereEveryPoiAlreadyHasAnImage() {
        Destination paris = Destination.builder()
            .id(UUID.randomUUID()).slug("paris").name("Paris").city("Paris").country("França")
            .active(true).build();

        Poi torreEiffel = Poi.builder().id(UUID.randomUUID()).destination(paris)
            .title("Torre Eiffel").category(PoiCategory.LANDMARK).budgetTier(BudgetTier.MODERATE)
            .active(true).images(new java.util.ArrayList<>(List.of("https://lh3.googleusercontent.com/existing")))
            .build();

        when(destinationRepo.findAllByActiveTrue()).thenReturn(List.of(paris));
        when(poiRepo.findAllByDestinationIdAndActiveTrue(paris.getId())).thenReturn(List.of(torreEiffel));

        PoiEnrichmentReport report = service.enrichExistingPoisWithImages();

        verifyNoInteractions(placesService);
        verify(poiRepo, never()).save(any(Poi.class));
        assertEquals(0, report.destinationsProcessed());
        assertEquals(1, report.destinationsSkipped());
    }

    @Test
    void enrichExistingPoisWithImages_shouldSkipPoi_withoutSaving_whenNoPhotoIsFound() {
        Destination madrid = Destination.builder()
            .id(UUID.randomUUID()).slug("madrid").name("Madrid").city("Madrid").country("Espanha")
            .active(true).build();

        Poi museuDoPrado = Poi.builder().id(UUID.randomUUID()).destination(madrid)
            .title("Museu do Prado").category(PoiCategory.MUSEUM).budgetTier(BudgetTier.MODERATE)
            .active(true).build();

        when(destinationRepo.findAllByActiveTrue()).thenReturn(List.of(madrid));
        when(poiRepo.findAllByDestinationIdAndActiveTrue(madrid.getId())).thenReturn(List.of(museuDoPrado));
        when(placesService.fetchPhotoUrl("Museu do Prado", "Madrid")).thenReturn(java.util.Optional.empty());

        PoiEnrichmentReport report = service.enrichExistingPoisWithImages();

        verify(poiRepo, never()).save(any(Poi.class));
        assertTrue(museuDoPrado.getImages().isEmpty());
        assertEquals(1, report.destinationsProcessed());
        assertEquals(0, report.totalPoisEnriched());
    }

    @Test
    void enrichExistingPoisWithImages_shouldNotStop_whenOneDestinationFails() {
        Destination lisboa = Destination.builder()
            .id(UUID.randomUUID()).slug("lisboa").name("Lisboa").city("Lisboa").country("Portugal")
            .active(true).build();
        Destination madrid = Destination.builder()
            .id(UUID.randomUUID()).slug("madrid").name("Madrid").city("Madrid").country("Espanha")
            .active(true).build();

        Poi torreDeBelem = Poi.builder().id(UUID.randomUUID()).destination(lisboa)
            .title("Torre de Belém").category(PoiCategory.MUSEUM).budgetTier(BudgetTier.MODERATE)
            .active(true).build();
        Poi puertaDelSol = Poi.builder().id(UUID.randomUUID()).destination(madrid)
            .title("Puerta del Sol").category(PoiCategory.PARK).budgetTier(BudgetTier.ECONOMY)
            .active(true).build();

        when(destinationRepo.findAllByActiveTrue()).thenReturn(List.of(lisboa, madrid));
        when(poiRepo.findAllByDestinationIdAndActiveTrue(lisboa.getId())).thenReturn(List.of(torreDeBelem));
        when(poiRepo.findAllByDestinationIdAndActiveTrue(madrid.getId())).thenReturn(List.of(puertaDelSol));
        when(placesService.fetchPhotoUrl("Torre de Belém", "Lisboa"))
            .thenThrow(new RuntimeException("Falha simulada no Google Places"));
        when(placesService.fetchPhotoUrl("Puerta del Sol", "Madrid"))
            .thenReturn(java.util.Optional.of("https://lh3.googleusercontent.com/puerta-del-sol"));
        when(poiRepo.save(any(Poi.class))).thenAnswer(inv -> inv.getArgument(0));

        PoiEnrichmentReport report = service.enrichExistingPoisWithImages();

        verify(poiRepo).save(argThat(p -> p.getId().equals(puertaDelSol.getId())));
        verify(poiRepo, never()).save(argThat(p -> p != null && p.getId().equals(torreDeBelem.getId())));
        assertEquals(1, report.destinationsProcessed());
        assertEquals(1, report.destinationsSkipped());
        assertTrue(report.processedSlugs().contains("madrid"));
        assertFalse(report.processedSlugs().contains("lisboa"));
    }

    // ─── enrichExistingPoisWithNormalizedTitleLanguage ────────────────────────
    //
    // Mesmo padrão de enrichExistingPoisWithMustSeeAndLandmark, mas SEM o
    // skip condicional: aqui TODO destino ativo com POIs é processado
    // (diferença: o problema de idioma já existe no catálogo inteiro, não é
    // condicional a um estado específico como "nenhum mustSee ainda").

    @Test
    void enrichExistingPoisWithNormalizedTitleLanguage_shouldApplyChanges_inPlace() {
        Destination budapeste = Destination.builder()
            .id(UUID.randomUUID()).slug("budapeste").name("Budapeste").city("Budapeste").country("Hungria")
            .active(true).build();

        Poi fishermansBastion = Poi.builder().id(UUID.randomUUID()).destination(budapeste)
            .title("Fisherman's Bastion").category(PoiCategory.LANDMARK).budgetTier(BudgetTier.MODERATE)
            .active(true).build();
        Poi basilicaSantoEstevao = Poi.builder().id(UUID.randomUUID()).destination(budapeste)
            .title("Basílica de Santo Estêvão").category(PoiCategory.LANDMARK).budgetTier(BudgetTier.MODERATE)
            .active(true).build();

        when(destinationRepo.findAllByActiveTrue()).thenReturn(List.of(budapeste));
        when(poiRepo.findAllByDestinationIdAndActiveTrue(budapeste.getId()))
            .thenReturn(List.of(fishermansBastion, basilicaSantoEstevao));
        when(llmService.normalizePoiTitleLanguage(eq("Budapeste"), eq("Hungria"), anyList()))
            .thenReturn(new PoiTitleNormalization(
                List.of(new PoiTitleChange("Fisherman's Bastion", "Bastião dos Pescadores"))
            ));
        when(poiRepo.save(any(Poi.class))).thenAnswer(inv -> inv.getArgument(0));

        PoiEnrichmentReport report = service.enrichExistingPoisWithNormalizedTitleLanguage();

        verify(poiRepo).save(argThat(p -> p.getId().equals(fishermansBastion.getId())
            && p.getTitle().equals("Bastião dos Pescadores")));
        verify(poiRepo, never()).save(argThat(p -> p != null && p.getId().equals(basilicaSantoEstevao.getId())));

        assertEquals(1, report.destinationsProcessed());
        assertEquals(0, report.destinationsSkipped());
        assertEquals(1, report.totalPoisEnriched());
        assertTrue(report.processedSlugs().contains("budapeste"));
    }

    @Test
    void enrichExistingPoisWithNormalizedTitleLanguage_shouldSkipDestination_withNoActivePois() {
        Destination edimburgo = Destination.builder()
            .id(UUID.randomUUID()).slug("edimburgo").name("Edimburgo").city("Edimburgo").country("Escócia")
            .active(true).build();

        when(destinationRepo.findAllByActiveTrue()).thenReturn(List.of(edimburgo));
        when(poiRepo.findAllByDestinationIdAndActiveTrue(edimburgo.getId())).thenReturn(List.of());

        PoiEnrichmentReport report = service.enrichExistingPoisWithNormalizedTitleLanguage();

        verify(llmService, never()).normalizePoiTitleLanguage(anyString(), anyString(), anyList());
        assertEquals(0, report.destinationsProcessed());
        assertEquals(1, report.destinationsSkipped());
    }

    @Test
    void enrichExistingPoisWithNormalizedTitleLanguage_shouldIgnoreHallucinatedTitle_withoutModifyingAnyPoi() {
        Destination faro = Destination.builder()
            .id(UUID.randomUUID()).slug("faro").name("Faro").city("Faro").country("Portugal")
            .active(true).build();

        Poi se = Poi.builder().id(UUID.randomUUID()).destination(faro)
            .title("Sé de Faro").category(PoiCategory.LANDMARK).budgetTier(BudgetTier.MODERATE)
            .active(true).build();

        when(destinationRepo.findAllByActiveTrue()).thenReturn(List.of(faro));
        when(poiRepo.findAllByDestinationIdAndActiveTrue(faro.getId())).thenReturn(List.of(se));
        when(llmService.normalizePoiTitleLanguage(eq("Faro"), eq("Portugal"), anyList()))
            .thenReturn(new PoiTitleNormalization(
                List.of(new PoiTitleChange("Título Inventado Pelo LLM", "Qualquer Coisa"))
            ));

        PoiEnrichmentReport report = service.enrichExistingPoisWithNormalizedTitleLanguage();

        verify(poiRepo, never()).save(any(Poi.class));
        assertEquals("Sé de Faro", se.getTitle());
        assertEquals(0, report.totalPoisEnriched());
    }

    @Test
    void enrichExistingPoisWithNormalizedTitleLanguage_shouldNotStop_whenOneDestinationFails() {
        Destination lisboa = Destination.builder()
            .id(UUID.randomUUID()).slug("lisboa").name("Lisboa").city("Lisboa").country("Portugal")
            .active(true).build();
        Destination madrid = Destination.builder()
            .id(UUID.randomUUID()).slug("madrid").name("Madrid").city("Madrid").country("Espanha")
            .active(true).build();

        Poi torreDeBelem = Poi.builder().id(UUID.randomUUID()).destination(lisboa)
            .title("Torre de Belém").category(PoiCategory.MUSEUM).budgetTier(BudgetTier.MODERATE)
            .active(true).build();
        Poi puertaDelSol = Poi.builder().id(UUID.randomUUID()).destination(madrid)
            .title("Puerta del Sol").category(PoiCategory.PARK).budgetTier(BudgetTier.ECONOMY)
            .active(true).build();

        when(destinationRepo.findAllByActiveTrue()).thenReturn(List.of(lisboa, madrid));
        when(poiRepo.findAllByDestinationIdAndActiveTrue(lisboa.getId())).thenReturn(List.of(torreDeBelem));
        when(poiRepo.findAllByDestinationIdAndActiveTrue(madrid.getId())).thenReturn(List.of(puertaDelSol));
        when(llmService.normalizePoiTitleLanguage(eq("Lisboa"), eq("Portugal"), anyList()))
            .thenThrow(new RuntimeException("Falha simulada do LLM"));
        when(llmService.normalizePoiTitleLanguage(eq("Madrid"), eq("Espanha"), anyList()))
            .thenReturn(new PoiTitleNormalization(
                List.of(new PoiTitleChange("Puerta del Sol", "Porta do Sol"))
            ));
        when(poiRepo.save(any(Poi.class))).thenAnswer(inv -> inv.getArgument(0));

        PoiEnrichmentReport report = service.enrichExistingPoisWithNormalizedTitleLanguage();

        verify(poiRepo).save(argThat(p -> p.getId().equals(puertaDelSol.getId())
            && p.getTitle().equals("Porta do Sol")));
        verify(poiRepo, never()).save(argThat(p -> p != null && p.getId().equals(torreDeBelem.getId())));

        assertEquals(1, report.destinationsProcessed());
        assertEquals(1, report.destinationsSkipped());
        assertTrue(report.processedSlugs().contains("madrid"));
        assertFalse(report.processedSlugs().contains("lisboa"));
    }

    // ─── generatePoisForExistingDestination: adiciona POIs a destino já existente ──

    @Test
    void generatePoisForExistingDestination_shouldAddPois_toDestinationThatAlreadyHasSome() {
        when(destinationRepo.findBySlug("copenhague")).thenReturn(java.util.Optional.of(destination));
        Poi jaExistente = Poi.builder().id(UUID.randomUUID()).title("Tivoli Gardens")
            .category(PoiCategory.PARK).active(true).destination(destination).build();
        when(poiRepo.findAllByDestinationIdAndActiveTrue(destination.getId()))
            .thenReturn(List.of(jaExistente));

        GeneratedPoi novoPoi = new GeneratedPoi(
            "Nyhavn", "LANDMARK", "Canal histórico colorido", "Nyhavn, Copenhagen",
            55.6797, 12.5908, "ECONOMY", 45, null, null, List.of("historia"), true, false, false
        );
        when(llmService.generatePois("Copenhagen", "Dinamarca", null, List.of("Tivoli Gardens")))
            .thenReturn(new PoisWrapper(List.of(novoPoi)));
        when(poiRepo.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        var report = service.generatePoisForExistingDestination("copenhague");

        assertEquals("copenhague", report.destinationSlug());
        assertEquals(1, report.poisGenerated());
        verify(poiRepo).saveAll(anyList());
        verify(llmService).generatePois("Copenhagen", "Dinamarca", null, List.of("Tivoli Gardens"));
    }

    @Test
    void generatePoisForExistingDestination_shouldThrowResourceNotFound_whenSlugDoesNotExist() {
        when(destinationRepo.findBySlug("inexistente")).thenReturn(java.util.Optional.empty());

        assertThrows(luvoygroup.voyage.exception.ResourceNotFoundException.class,
            () -> service.generatePoisForExistingDestination("inexistente"));

        verify(llmService, never()).generatePois(anyString(), anyString(), any(), anyList());
    }

    // ─── generatePoisForExistingDestination(slug, focusCategory): reforço focado numa categoria ──

    @Test
    void generatePoisForExistingDestination_withFocusCategory_shouldPassExistingTitles_andFocusToLlm() {
        when(destinationRepo.findBySlug("copenhague")).thenReturn(java.util.Optional.of(destination));
        Poi jaExistente = Poi.builder().id(UUID.randomUUID()).title("Tivoli Gardens")
            .category(PoiCategory.PARK).active(true).destination(destination).build();
        when(poiRepo.findAllByDestinationIdAndActiveTrue(destination.getId()))
            .thenReturn(List.of(jaExistente));

        GeneratedPoi novoPoi = new GeneratedPoi(
            "Noma", "RESTAURANT", "Restaurante premiado", "Refshalevej 96, Copenhagen",
            55.6838, 12.6089, "LUXURY", 120, null, null, List.of("gastronomia"), false, false, false
        );
        when(llmService.generatePois("Copenhagen", "Dinamarca", "RESTAURANT", List.of("Tivoli Gardens")))
            .thenReturn(new PoisWrapper(List.of(novoPoi)));
        when(poiRepo.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        var report = service.generatePoisForExistingDestination("copenhague", PoiCategory.RESTAURANT);

        assertEquals("copenhague", report.destinationSlug());
        assertEquals(1, report.poisGenerated());
        verify(llmService).generatePois("Copenhagen", "Dinamarca", "RESTAURANT", List.of("Tivoli Gardens"));
    }
}
