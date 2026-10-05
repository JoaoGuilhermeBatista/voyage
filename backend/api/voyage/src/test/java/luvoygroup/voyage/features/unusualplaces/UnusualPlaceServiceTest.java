package luvoygroup.voyage.features.unusualplaces;

import luvoygroup.voyage.domain.Destination;
import luvoygroup.voyage.domain.Poi;
import luvoygroup.voyage.domain.UnusualPlace;
import luvoygroup.voyage.domain.User;
import luvoygroup.voyage.domain.enums.BudgetTier;
import luvoygroup.voyage.domain.enums.PoiCategory;
import luvoygroup.voyage.exception.ResourceNotFoundException;
import luvoygroup.voyage.features.admin.LlmService;
import luvoygroup.voyage.features.unusualplaces.dto.GeneratedUnusualPlace;
import luvoygroup.voyage.features.unusualplaces.dto.UnusualPlaceDTO;
import luvoygroup.voyage.features.unusualplaces.dto.UnusualPlacesWrapper;
import luvoygroup.voyage.repository.DestinationRepository;
import luvoygroup.voyage.repository.PoiRepository;
import luvoygroup.voyage.repository.UnusualPlaceRepository;
import luvoygroup.voyage.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários "standalone" de UnusualPlaceService — mesmo padrão de
 * ItineraryTemplateServiceTest/AdminServiceTest: sem @ExtendWith(MockitoExtension),
 * mocks via Mockito.mock() direto, service instanciado via construtor Lombok.
 */
class UnusualPlaceServiceTest {

    private LlmService llmService;
    private DestinationRepository destinationRepo;
    private PoiRepository poiRepo;
    private UnusualPlaceRepository unusualPlaceRepo;
    private UserRepository userRepo;

    private UnusualPlaceService service;

    private Destination destination;

    @BeforeEach
    void setUp() {
        llmService = mock(LlmService.class);
        destinationRepo = mock(DestinationRepository.class);
        poiRepo = mock(PoiRepository.class);
        unusualPlaceRepo = mock(UnusualPlaceRepository.class);
        userRepo = mock(UserRepository.class);

        service = new UnusualPlaceService(llmService, destinationRepo, poiRepo, unusualPlaceRepo, userRepo);

        destination = Destination.builder()
            .id(UUID.randomUUID())
            .slug("paris")
            .name("Paris")
            .city("Paris")
            .country("França")
            .active(true)
            .build();
    }

    // ─── generateForDestination ───────────────────────────────────────────

    @Test
    void generateForDestination_shouldDeleteOldPlaces_andSaveNewOnesFromLlm() {
        when(destinationRepo.findBySlug("paris")).thenReturn(Optional.of(destination));

        Poi existingPoi = Poi.builder()
            .id(UUID.randomUUID())
            .destination(destination)
            .title("Torre Eiffel")
            .category(PoiCategory.LANDMARK)
            .budgetTier(BudgetTier.MODERATE)
            .build();
        when(poiRepo.findAllByDestinationIdAndActiveTrue(destination.getId())).thenReturn(List.of(existingPoi));

        UnusualPlace existingUnusualPlace = UnusualPlace.builder()
            .id(UUID.randomUUID())
            .destination(destination)
            .title("Antigo")
            .description("desc")
            .orderIndex(0)
            .build();
        when(unusualPlaceRepo.findAllByDestinationIdOrderByOrderIndexAsc(destination.getId()))
            .thenReturn(List.of(existingUnusualPlace));

        UnusualPlacesWrapper wrapper = new UnusualPlacesWrapper(List.of(
            new GeneratedUnusualPlace("Bar Escondido", "Um bar sem placa, só quem mora perto conhece.", "Bar escondido"),
            new GeneratedUnusualPlace("Mirante Secreto", "Vista da cidade sem fila nenhuma.", "Mirante local")
        ));
        when(llmService.generateUnusualPlaces(eq("Paris"), eq("França"), anyList())).thenReturn(wrapper);

        int count = service.generateForDestination("paris");

        assertEquals(2, count);
        verify(unusualPlaceRepo).deleteAll(List.of(existingUnusualPlace));

        @SuppressWarnings("unchecked")
        var captor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(unusualPlaceRepo).saveAll(captor.capture());
        List<UnusualPlace> saved = captor.getValue();
        assertEquals(2, saved.size());
        assertEquals("Bar Escondido", saved.get(0).getTitle());
        assertEquals(0, saved.get(0).getOrderIndex());
        assertEquals("Mirante Secreto", saved.get(1).getTitle());
        assertEquals(1, saved.get(1).getOrderIndex());
    }

    @Test
    void generateForDestination_shouldThrowResourceNotFound_whenSlugDoesNotExist() {
        when(destinationRepo.findBySlug("inexistente")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.generateForDestination("inexistente"));
        verifyNoInteractions(llmService, unusualPlaceRepo);
    }

    @Test
    void generateForDestination_shouldThrowIllegalState_whenLlmReturnsEmpty() {
        when(destinationRepo.findBySlug("paris")).thenReturn(Optional.of(destination));
        when(poiRepo.findAllByDestinationIdAndActiveTrue(destination.getId())).thenReturn(List.of());
        when(llmService.generateUnusualPlaces(anyString(), anyString(), anyList()))
            .thenReturn(new UnusualPlacesWrapper(List.of()));

        assertThrows(IllegalStateException.class, () -> service.generateForDestination("paris"));
        verify(unusualPlaceRepo, never()).saveAll(anyList());
    }

    // ─── findForUser ──────────────────────────────────────────────────────

    @Test
    void findForUser_shouldReturnTeaser_whenUserHasNoActivePremium() {
        when(destinationRepo.findBySlug("paris")).thenReturn(Optional.of(destination));

        UUID userId = UUID.randomUUID();
        User freeUser = User.builder().id(userId).isPremium(false).build();
        when(userRepo.findById(userId)).thenReturn(Optional.of(freeUser));

        UnusualPlace place = UnusualPlace.builder()
            .destination(destination)
            .title("Bar Escondido")
            .description("Descrição completa.")
            .category("Bar escondido")
            .orderIndex(0)
            .build();
        when(unusualPlaceRepo.findAllByDestinationIdOrderByOrderIndexAsc(destination.getId()))
            .thenReturn(List.of(place));

        List<UnusualPlaceDTO> result = service.findForUser("paris", userId);

        assertEquals(1, result.size());
        assertTrue(result.get(0).locked());
        assertEquals("Bar Escondido", result.get(0).title());
        assertNull(result.get(0).description());
        assertNull(result.get(0).category());
    }

    @Test
    void findForUser_shouldReturnFullContent_whenUserHasActivePremium() {
        when(destinationRepo.findBySlug("paris")).thenReturn(Optional.of(destination));

        UUID userId = UUID.randomUUID();
        User premiumUser = User.builder().id(userId).isPremium(true)
            .premiumExpiresAt(LocalDateTime.now().plusDays(10)).build();
        when(userRepo.findById(userId)).thenReturn(Optional.of(premiumUser));

        UnusualPlace place = UnusualPlace.builder()
            .destination(destination)
            .title("Bar Escondido")
            .description("Descrição completa.")
            .category("Bar escondido")
            .orderIndex(0)
            .build();
        when(unusualPlaceRepo.findAllByDestinationIdOrderByOrderIndexAsc(destination.getId()))
            .thenReturn(List.of(place));

        List<UnusualPlaceDTO> result = service.findForUser("paris", userId);

        assertEquals(1, result.size());
        assertFalse(result.get(0).locked());
        assertEquals("Bar Escondido", result.get(0).title());
        assertEquals("Descrição completa.", result.get(0).description());
        assertEquals("Bar escondido", result.get(0).category());
    }

    @Test
    void findForUser_shouldThrowResourceNotFound_whenDestinationInactive() {
        destination.setActive(false);
        when(destinationRepo.findBySlug("paris")).thenReturn(Optional.of(destination));

        assertThrows(ResourceNotFoundException.class, () -> service.findForUser("paris", UUID.randomUUID()));
    }

    @Test
    void findForUser_shouldThrowResourceNotFound_whenUserDoesNotExist() {
        when(destinationRepo.findBySlug("paris")).thenReturn(Optional.of(destination));
        UUID userId = UUID.randomUUID();
        when(userRepo.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findForUser("paris", userId));
    }
}
