package luvoygroup.voyage.features.favorites.dto;

import java.util.UUID;

public record FavoriteRequest(
    UUID destinationId,
    UUID poiId,
    UUID itineraryId
) {}
