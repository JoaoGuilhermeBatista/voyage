package luvoygroup.voyage.features.users;

import jakarta.validation.Valid;
import luvoygroup.voyage.features.users.dto.UpdatePreferencesRequest;
import luvoygroup.voyage.features.users.dto.UserProfileDTO;
import luvoygroup.voyage.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public UserProfileDTO getMe(@AuthenticationPrincipal AuthenticatedUser principal) {
        return userService.upsertAndGetProfile(
            UUID.fromString(principal.id()),
            principal.email()
        );
    }

    @PatchMapping("/preferences")
    public Map<String, String> updatePreferences(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @Valid @RequestBody UpdatePreferencesRequest request
    ) {
        UUID userId = UUID.fromString(principal.id());
        userService.updatePreferences(userId, request);
        return Map.of("message", "Preferences updated");
    }
}
