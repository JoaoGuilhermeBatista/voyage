package luvoygroup.voyage.features.users.dto;

import luvoygroup.voyage.domain.OnboardingPreferences;
import luvoygroup.voyage.domain.User;

import java.util.List;
import java.util.UUID;

public record UserProfileDTO(
    UUID id,
    String email,
    String avatarUrl,
    boolean onboardingCompleted,
    boolean isPremium,
    String budget,
    String rhythm,
    String groupType,
    List<String> interests
) {
    public static UserProfileDTO from(User user) {
        return new UserProfileDTO(
            user.getId(),
            user.getEmail(),
            user.getAvatarUrl(),
            user.isOnboardingCompleted(),
            user.isPremium(),
            null,
            null,
            null,
            null
        );
    }

    public static UserProfileDTO fromUserAndPrefs(User user, OnboardingPreferences prefs) {
        return new UserProfileDTO(
            user.getId(),
            user.getEmail(),
            user.getAvatarUrl(),
            user.isOnboardingCompleted(),
            user.isPremium(),
            prefs != null && prefs.getBudget() != null ? prefs.getBudget().name() : null,
            prefs != null && prefs.getRhythm() != null ? prefs.getRhythm().name() : null,
            prefs != null && prefs.getGroupType() != null ? prefs.getGroupType().name() : null,
            prefs != null ? prefs.getInterests() : null
        );
    }
}
