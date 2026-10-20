package luvoygroup.voyage.features.users;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import luvoygroup.voyage.domain.User;
import luvoygroup.voyage.repository.OnboardingPreferencesRepository;
import luvoygroup.voyage.repository.UserRepository;
import luvoygroup.voyage.features.users.dto.UserProfileDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Testes unitários "standalone" de UserService — mesmo padrão de
 * ItineraryServiceTest/BillingServiceTest (mocks via Mockito.mock() direto,
 * sem @ExtendWith(MockitoExtension.class)).
 *
 * Foco: o bug real reproduzido em produção (2026-10-04) — o Supabase Auth
 * recriou a identidade (novo auth.users.id) de uma conta de teste que
 * reusava o e-mail de uma conta antiga já existente em public.users.
 * upsertAndGetProfile (chamado por GET /users/me, que o Flutter sempre roda
 * ANTES de salvar preferências) tentava então inserir um novo User com um
 * e-mail que já era UNIQUE em outra linha, estourando a constraint do banco
 * — surgia pro usuário como "Erro ao salvar suas preferências. Tente
 * novamente." tanto no onboarding quanto na edição de preferências, porque
 * as duas telas chamam fetchMe()/GET /users/me antes do PATCH.
 */
class UserServiceTest {

    private UserRepository userRepo;
    private OnboardingPreferencesRepository prefsRepo;
    private EntityManager entityManager;
    private Query nativeQuery;

    private UserService service;

    private static final UUID OLD_ID = UUID.randomUUID();
    private static final UUID NEW_ID = UUID.randomUUID();
    private static final String EMAIL = "joaoguilhermebatistamoura@gmail.com";

    @BeforeEach
    void setUp() {
        userRepo = mock(UserRepository.class);
        prefsRepo = mock(OnboardingPreferencesRepository.class);
        entityManager = mock(EntityManager.class);
        nativeQuery = mock(Query.class);

        when(entityManager.createNativeQuery(anyString())).thenReturn(nativeQuery);
        when(nativeQuery.setParameter(anyString(), any())).thenReturn(nativeQuery);
        when(nativeQuery.executeUpdate()).thenReturn(1);

        when(prefsRepo.findByUserId(any())).thenReturn(Optional.empty());

        service = new UserService(userRepo, prefsRepo, entityManager);
    }

    // ─── Caminho feliz já existente: id e e-mail não existem ainda ───────────

    @Test
    void upsertAndGetProfile_shouldCreateNewUser_whenNeitherIdNorEmailExists() {
        when(userRepo.findById(NEW_ID)).thenReturn(Optional.empty());
        when(userRepo.findByEmail(EMAIL)).thenReturn(Optional.empty());

        User saved = User.builder().id(NEW_ID).email(EMAIL).name("joaoguilhermebatistamoura").build();
        when(userRepo.save(any(User.class))).thenReturn(saved);

        UserProfileDTO profile = service.upsertAndGetProfile(NEW_ID, EMAIL);

        assertEquals(NEW_ID, profile.id());
        verify(userRepo).save(any(User.class));
        verifyNoInteractions(entityManager);
    }

    // ─── Caminho feliz já existente: id já existe ────────────────────────────

    @Test
    void upsertAndGetProfile_shouldReturnExistingUser_whenIdMatches() {
        User existing = User.builder().id(NEW_ID).email(EMAIL).name("joao").onboardingCompleted(true).build();
        when(userRepo.findById(NEW_ID)).thenReturn(Optional.of(existing));

        UserProfileDTO profile = service.upsertAndGetProfile(NEW_ID, EMAIL);

        assertEquals(NEW_ID, profile.id());
        assertTrue(profile.onboardingCompleted());
        verify(userRepo, never()).save(any());
        verifyNoInteractions(entityManager);
    }

    // ─── O bug de verdade: auth.users.id mudou, e-mail já pertence a outra linha ─

    @Test
    void upsertAndGetProfile_shouldReconcileAuthIdentity_whenAuthIdChangedButEmailAlreadyExists() {
        User existingUnderOldId = User.builder()
            .id(OLD_ID)
            .email(EMAIL)
            .name("joao")
            .onboardingCompleted(true)
            .isPremium(true)
            .premiumExpiresAt(LocalDateTime.now().plusDays(10))
            .build();

        User migratedUnderNewId = User.builder()
            .id(NEW_ID)
            .email(EMAIL)
            .name("joao")
            .onboardingCompleted(true)
            .isPremium(true)
            .premiumExpiresAt(existingUnderOldId.getPremiumExpiresAt())
            .build();

        when(userRepo.findById(NEW_ID))
            .thenReturn(Optional.empty())
            .thenReturn(Optional.of(migratedUnderNewId));
        when(userRepo.findByEmail(EMAIL)).thenReturn(Optional.of(existingUnderOldId));

        UserProfileDTO profile = service.upsertAndGetProfile(NEW_ID, EMAIL);

        // Não tenta inserir uma linha nova e duplicar o e-mail.
        verify(userRepo, never()).save(any());

        // Migra pelas 6 instruções nativas, na ordem: libera e-mail antigo,
        // cria linha nova, migra favoritos/roteiros/preferências, apaga a antiga.
        verify(entityManager, times(6)).createNativeQuery(anyString());
        verify(nativeQuery, times(6)).executeUpdate();
        verify(entityManager).flush();
        verify(entityManager).clear();

        // O perfil devolvido reflete a identidade NOVA, com os dados antigos preservados.
        assertEquals(NEW_ID, profile.id());
        assertEquals(EMAIL, profile.email());
        assertTrue(profile.onboardingCompleted());
        assertTrue(profile.isPremium());
    }
}
