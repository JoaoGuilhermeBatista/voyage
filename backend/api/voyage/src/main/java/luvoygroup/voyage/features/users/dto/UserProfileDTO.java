package luvoygroup.voyage.features.users.dto;

import luvoygroup.voyage.domain.OnboardingPreferences;
import luvoygroup.voyage.domain.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record UserProfileDTO(
    UUID id,
    String email,
    String avatarUrl,
    boolean onboardingCompleted,
    boolean isPremium,
    LocalDateTime premiumExpiresAt,
    String budget,
    String rhythm,
    String groupType,
    List<String> interests,
    String religion
) {
    public static UserProfileDTO fromUserAndPrefs(User user, OnboardingPreferences prefs) {
        return new UserProfileDTO(
            user.getId(),
            user.getEmail(),
            user.getAvatarUrl(),
            user.isOnboardingCompleted(),
            // hasActivePremium() (não isPremium() cru): premium via Pix nunca
            // "desliga" a flag sozinho — sem isso, o app continuaria
            // mostrando "Premium" mesmo depois de vencer o prazo do Pix.
            user.hasActivePremium(),
            user.getPremiumExpiresAt(),
            prefs != null && prefs.getBudget() != null ? prefs.getBudget().name() : null,
            prefs != null && prefs.getRhythm() != null ? prefs.getRhythm().name() : null,
            prefs != null && prefs.getGroupType() != null ? prefs.getGroupType().name() : null,
            prefs != null ? prefs.getInterests() : null,
            prefs != null && prefs.getReligion() != null ? prefs.getReligion().name() : null
        );
    }
}
