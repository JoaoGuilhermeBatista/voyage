package luvoygroup.voyage.features.users;

import jakarta.persistence.EntityManager;
import luvoygroup.voyage.domain.OnboardingPreferences;
import luvoygroup.voyage.domain.User;
import luvoygroup.voyage.exception.ResourceNotFoundException;
import luvoygroup.voyage.features.users.dto.UpdatePreferencesRequest;
import luvoygroup.voyage.features.users.dto.UserProfileDTO;
import luvoygroup.voyage.repository.OnboardingPreferencesRepository;
import luvoygroup.voyage.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final OnboardingPreferencesRepository prefsRepository;
    private final EntityManager entityManager;

    @Transactional
    public UserProfileDTO upsertAndGetProfile(UUID supabaseId, String email) {
        User user = userRepository.findById(supabaseId).orElseGet(() ->
            userRepository.findByEmail(email)
                .map(existing -> reconcileAuthIdentity(existing, supabaseId))
                .orElseGet(() -> userRepository.save(User.builder()
                    .id(supabaseId)
                    .email(email)
                    .name(email.split("@")[0])
                    .build()))
        );
        OnboardingPreferences prefs = prefsRepository.findByUserId(user.getId()).orElse(null);
        return UserProfileDTO.fromUserAndPrefs(user, prefs);
    }

    /**
     * O Supabase Auth pode recriar a identidade de um e-mail (novo
     * auth.users.id) sem avisar a gente — aconteceu de verdade com uma conta
     * de teste apagada/recriada durante a configuração de SMTP (ver
     * luvoy-pendencias.md). Quando isso acontece, o JWT novo chega com um
     * "sub" (supabaseId) diferente do id já salvo aqui pro mesmo e-mail, e um
     * INSERT ingênuo bateria na constraint UNIQUE de email — é exatamente o
     * "erro ao salvar preferências" que apareceu pro usuário, porque o fetch
     * de perfil que roda antes (GET /users/me) já falhava aqui.
     *
     * Em vez de duplicar/falhar, migramos a linha existente (e os dados
     * dependentes: favoritos, roteiros, preferências de onboarding) pro novo
     * id, preservando o histórico do mesmo e-mail. Via SQL nativo porque
     * trocar a PK de uma entidade gerenciada não é algo que o JPA suporte
     * (precisa existir a linha com o novo id ANTES de repontar as FKs
     * — nenhuma delas é DEFERRABLE em produção — daí a ordem: libera o
     * e-mail antigo, cria a linha nova, migra as FKs, só então apaga a
     * antiga).
     */
    private User reconcileAuthIdentity(User existing, UUID newId) {
        UUID oldId = existing.getId();
        String email = existing.getEmail();

        entityManager.createNativeQuery(
                "UPDATE users SET email = email || '__migrating__' || id::text WHERE id = :oldId")
            .setParameter("oldId", oldId)
            .executeUpdate();

        entityManager.createNativeQuery("""
                INSERT INTO users (id, email, name, avatar_url, onboarding_completed,
                    is_premium, pdf_export_count, extra_pdf_credits, stripe_customer_id,
                    stripe_subscription_id, premium_expires_at, created_at, updated_at, deleted_at)
                SELECT :newId, :email, name, avatar_url, onboarding_completed,
                    is_premium, pdf_export_count, extra_pdf_credits, stripe_customer_id,
                    stripe_subscription_id, premium_expires_at, created_at, now(), deleted_at
                FROM users WHERE id = :oldId
                """)
            .setParameter("newId", newId)
            .setParameter("email", email)
            .setParameter("oldId", oldId)
            .executeUpdate();

        entityManager.createNativeQuery("UPDATE favorites SET user_id = :newId WHERE user_id = :oldId")
            .setParameter("newId", newId).setParameter("oldId", oldId).executeUpdate();
        entityManager.createNativeQuery("UPDATE itineraries SET user_id = :newId WHERE user_id = :oldId")
            .setParameter("newId", newId).setParameter("oldId", oldId).executeUpdate();
        entityManager.createNativeQuery("UPDATE onboarding_preferences SET user_id = :newId WHERE user_id = :oldId")
            .setParameter("newId", newId).setParameter("oldId", oldId).executeUpdate();

        entityManager.createNativeQuery("DELETE FROM users WHERE id = :oldId")
            .setParameter("oldId", oldId).executeUpdate();

        entityManager.flush();
        entityManager.clear();

        return userRepository.findById(newId).orElseThrow();
    }

    @Transactional
    public void updatePreferences(UUID userId, UpdatePreferencesRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(ResourceNotFoundException::new);

        OnboardingPreferences prefs = prefsRepository.findByUserId(userId)
            .orElse(OnboardingPreferences.builder().user(user).build());

        prefs.setBudget(request.budget());
        prefs.setRhythm(request.rhythm());
        prefs.setGroupType(request.groupType());
        prefs.setInterests(request.interests());
        prefs.setReligion(request.religion());

        prefsRepository.save(prefs);

        user.setOnboardingCompleted(true);
        userRepository.save(user);
    }
}
