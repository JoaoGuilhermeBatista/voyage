package luvoygroup.voyage.features.favorites.dto;

import luvoygroup.voyage.domain.Favorite;
import luvoygroup.voyage.features.destinations.dto.DestinationSummaryDTO;

import java.time.LocalDateTime;
import java.util.UUID;

public record FavoriteListItemDTO(
    UUID id,
    LocalDateTime createdAt,
    DestinationSummaryDTO destination,
    PoiFavoriteDTO poi,
    ItineraryFavoriteDTO itinerary
) {
    public static FavoriteListItemDTO from(Favorite f) {
        DestinationSummaryDTO dest = f.getDestination() != null
            ? DestinationSummaryDTO.from(f.getDestination()) : null;
        PoiFavoriteDTO poi = f.getPoi() != null
            ? new PoiFavoriteDTO(f.getPoi().getId(), f.getPoi().getTitle(), f.getPoi().getCategory()) : null;
        ItineraryFavoriteDTO itin = f.getItinerary() != null
            ? new ItineraryFavoriteDTO(f.getItinerary().getId(), f.getItinerary().getTitle()) : null;
        return new FavoriteListItemDTO(f.getId(), f.getCreatedAt(), dest, poi, itin);
    }
}
