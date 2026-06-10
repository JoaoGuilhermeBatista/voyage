package luvoygroup.voyage.repository;

import luvoygroup.voyage.domain.OnboardingPreferences;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OnboardingPreferencesRepository extends JpaRepository<OnboardingPreferences, UUID> {
    Optional<OnboardingPreferences> findByUserId(UUID userId);
}
