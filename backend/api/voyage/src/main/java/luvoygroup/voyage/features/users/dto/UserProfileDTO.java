package luvoygroup.voyage.features.users.dto;

import luvoygroup.voyage.domain.User;

import java.util.UUID;

public record UserProfileDTO(
    UUID id,
    String email,
    String avatarUrl,
    boolean onboardingCompleted,
    boolean isPremium
) {
    public static UserProfileDTO from(User user) {
        return new UserProfileDTO(
            user.getId(),
            user.getEmail(),
            user.getAvatarUrl(),
            user.isOnboardingCompleted(),
            user.isPremium()
        );
    }
}
