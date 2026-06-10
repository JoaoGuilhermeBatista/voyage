package luvoygroup.voyage.features.auth.dto;

import luvoygroup.voyage.domain.User;

import java.util.UUID;

public record UserDTO(
    UUID id,
    String name,
    String email,
    boolean onboardingCompleted
) {
    public static UserDTO from(User user) {
        return new UserDTO(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.isOnboardingCompleted()
        );
    }
}
