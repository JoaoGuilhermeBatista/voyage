package luvoygroup.voyage.features.admin;

import luvoygroup.voyage.domain.Destination;
import luvoygroup.voyage.domain.Poi;
import luvoygroup.voyage.domain.enums.PoiCategory;
import luvoygroup.voyage.features.admin.dto.GeneratedPoi;
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
            true
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
    }
}
