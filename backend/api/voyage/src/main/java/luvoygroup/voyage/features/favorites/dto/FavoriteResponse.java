package luvoygroup.voyage.features.favorites.dto;

import luvoygroup.voyage.domain.Favorite;

import java.time.LocalDateTime;
import java.util.UUID;

public record FavoriteResponse(UUID id, LocalDateTime createdAt) {
    public static FavoriteResponse from(Favorite f) {
        return new FavoriteResponse(f.getId(), f.getCreatedAt());
    }
}
