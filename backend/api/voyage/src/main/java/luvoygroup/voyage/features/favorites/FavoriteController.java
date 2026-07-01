package luvoygroup.voyage.features.favorites;

import luvoygroup.voyage.features.favorites.dto.FavoriteListItemDTO;
import luvoygroup.voyage.features.favorites.dto.FavoriteRequest;
import luvoygroup.voyage.features.favorites.dto.FavoriteResponse;
import luvoygroup.voyage.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FavoriteResponse create(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @RequestBody FavoriteRequest request
    ) {
        UUID userId = UUID.fromString(principal.id());
        return favoriteService.create(userId, request);
    }

    @GetMapping
    public List<FavoriteListItemDTO> listAll(@AuthenticationPrincipal AuthenticatedUser principal) {
        UUID userId = UUID.fromString(principal.id());
        return favoriteService.findAll(userId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @PathVariable UUID id
    ) {
        UUID userId = UUID.fromString(principal.id());
        favoriteService.delete(userId, id);
    }
}
