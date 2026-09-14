package luvoygroup.voyage.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * hasActivePremium() é a fonte única de verdade de acesso premium (ver
 * BillingService/ItineraryService/PdfService) — testado isolado aqui
 * porque toda a robustez do sistema de pagamento depende desta única
 * checagem estar certa.
 */
class UserTest {

    private User userWith(boolean isPremium, LocalDateTime expiresAt) {
        return User.builder().isPremium(isPremium).premiumExpiresAt(expiresAt).build();
    }

    @Test
    void hasActivePremium_isFalse_forNonPremiumUser() {
        assertFalse(userWith(false, null).hasActivePremium());
    }

    @Test
    void hasActivePremium_isTrue_whenPremiumWithNoExpiryDate() {
        // Não deveria acontecer em produção (todo caminho de código sempre
        // seta uma data), mas null = "sem prazo" é o default seguro.
        assertTrue(userWith(true, null).hasActivePremium());
    }

    @Test
    void hasActivePremium_isTrue_whenPremiumAndExpiryIsInTheFuture() {
        assertTrue(userWith(true, LocalDateTime.now().plusDays(1)).hasActivePremium());
    }

    @Test
    void hasActivePremium_isFalse_whenPremiumFlagSetButExpiryAlreadyPassed() {
        // O caso central do Pix: isPremium nunca é "desligado" sozinho,
        // então sem essa checagem de data o acesso nunca expiraria.
        assertFalse(userWith(true, LocalDateTime.now().minusSeconds(1)).hasActivePremium());
    }
}
