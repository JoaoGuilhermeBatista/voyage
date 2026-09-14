package luvoygroup.voyage.features.pdf;

import luvoygroup.voyage.domain.Destination;
import luvoygroup.voyage.domain.Itinerary;
import luvoygroup.voyage.domain.User;
import luvoygroup.voyage.exception.ForbiddenAccessException;
import luvoygroup.voyage.exception.PaymentRequiredException;
import luvoygroup.voyage.exception.ResourceNotFoundException;
import luvoygroup.voyage.repository.ItineraryRepository;
import luvoygroup.voyage.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Regra de monetização do PDF (ver luvoy-pendencias.md): 1ª exportação
 * grátis pra todo mundo, depois só com premium ativo (cartão OU Pix,
 * indistintamente — via User.hasActivePremium()) ou 1 crédito avulso
 * comprado via Pix ("PDF novo").
 */
class PdfServiceTest {

    private ItineraryRepository itineraryRepo;
    private UserRepository userRepo;
    private PdfService service;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ITINERARY_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        itineraryRepo = mock(ItineraryRepository.class);
        userRepo = mock(UserRepository.class);
        service = new PdfService(itineraryRepo, userRepo);
    }

    private Itinerary itineraryOwnedByUser(UUID userId) {
        Destination destination = Destination.builder().id(UUID.randomUUID()).slug("paris").name("Paris")
            .country("França").city("Paris").tagline("").active(true).build();
        return Itinerary.builder()
            .id(ITINERARY_ID)
            .user(User.builder().id(userId).build())
            .destination(destination)
            .startDate(java.time.LocalDate.now())
            .endDate(java.time.LocalDate.now().plusDays(3))
            .title("Paris")
            .build();
    }

    private User freeUser() {
        return User.builder().id(USER_ID).isPremium(false).pdfExportCount(0).extraPdfCredits(0).build();
    }

    @Test
    void export_shouldSucceed_forFreeUserWithNoExportsYet_andIncrementCount() {
        when(itineraryRepo.findByIdAndDeletedAtIsNull(ITINERARY_ID))
            .thenReturn(Optional.of(itineraryOwnedByUser(USER_ID)));
        User user = freeUser();
        when(userRepo.findById(USER_ID)).thenReturn(Optional.of(user));

        var result = service.export(USER_ID, ITINERARY_ID);

        assertEquals("itineraries/" + ITINERARY_ID + ".pdf", result.get("pdfUrl"));
        assertEquals(1, user.getPdfExportCount());
        verify(userRepo).save(user);
    }

    @Test
    void export_shouldThrowPaymentRequired_whenFreeExportAlreadyUsed_andNoCreditsLeft() {
        when(itineraryRepo.findByIdAndDeletedAtIsNull(ITINERARY_ID))
            .thenReturn(Optional.of(itineraryOwnedByUser(USER_ID)));
        User user = freeUser();
        user.setPdfExportCount(1);
        when(userRepo.findById(USER_ID)).thenReturn(Optional.of(user));

        assertThrows(PaymentRequiredException.class, () -> service.export(USER_ID, ITINERARY_ID));
        verify(userRepo, never()).save(any());
    }

    @Test
    void export_shouldConsumeOneExtraCredit_whenFreeExportAlreadyUsed_insteadOfBlocking() {
        when(itineraryRepo.findByIdAndDeletedAtIsNull(ITINERARY_ID))
            .thenReturn(Optional.of(itineraryOwnedByUser(USER_ID)));
        User user = freeUser();
        user.setPdfExportCount(1);
        user.setExtraPdfCredits(2);
        when(userRepo.findById(USER_ID)).thenReturn(Optional.of(user));

        service.export(USER_ID, ITINERARY_ID);

        assertEquals(1, user.getExtraPdfCredits());
        assertEquals(1, user.getPdfExportCount()); // não mexe mais na contagem grátis
        verify(userRepo).save(user);
    }

    @Test
    void export_shouldNeverTouchCounters_forUserWithActivePremium_regardlessOfPaymentMethod() {
        when(itineraryRepo.findByIdAndDeletedAtIsNull(ITINERARY_ID))
            .thenReturn(Optional.of(itineraryOwnedByUser(USER_ID)));
        User user = freeUser();
        user.setPdfExportCount(1);
        user.setPremium(true);
        user.setPremiumExpiresAt(LocalDateTime.now().plusDays(5)); // premium via Pix, ainda válido
        when(userRepo.findById(USER_ID)).thenReturn(Optional.of(user));

        service.export(USER_ID, ITINERARY_ID);

        assertEquals(1, user.getPdfExportCount()); // inalterado
        verify(userRepo, never()).save(any());
    }

    @Test
    void export_shouldTreatExpiredPixPremium_asNotPremium_andEnforceTheLimitNormally() {
        when(itineraryRepo.findByIdAndDeletedAtIsNull(ITINERARY_ID))
            .thenReturn(Optional.of(itineraryOwnedByUser(USER_ID)));
        User user = freeUser();
        user.setPdfExportCount(1);
        user.setPremium(true);
        user.setPremiumExpiresAt(LocalDateTime.now().minusDays(1)); // Pix expirado, nunca renovado
        when(userRepo.findById(USER_ID)).thenReturn(Optional.of(user));

        assertThrows(PaymentRequiredException.class, () -> service.export(USER_ID, ITINERARY_ID));
    }

    @Test
    void export_shouldThrowForbidden_whenItineraryBelongsToAnotherUser() {
        when(itineraryRepo.findByIdAndDeletedAtIsNull(ITINERARY_ID))
            .thenReturn(Optional.of(itineraryOwnedByUser(UUID.randomUUID())));

        assertThrows(ForbiddenAccessException.class, () -> service.export(USER_ID, ITINERARY_ID));
        verifyNoInteractions(userRepo);
    }

    @Test
    void export_shouldThrowNotFound_whenItineraryDoesNotExist() {
        when(itineraryRepo.findByIdAndDeletedAtIsNull(ITINERARY_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.export(USER_ID, ITINERARY_ID));
    }
}
