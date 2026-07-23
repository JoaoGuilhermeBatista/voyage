package luvoygroup.voyage.features.itinerary.dto;

import luvoygroup.voyage.domain.Itinerary;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public record ItinerarySummaryDTO(
    UUID id,
    String title,
    UUID destinationId,
    String destinationName,
    String destinationHeroImageUrl,
    LocalDate startDate,
    LocalDate endDate,
    int dayCount
) {
    public static ItinerarySummaryDTO from(Itinerary it) {
        return new ItinerarySummaryDTO(
            it.getId(),
            it.getTitle(),
            it.getDestination().getId(),
            it.getDestination().getName(),
            it.getDestination().getHeroImageUrl(),
            it.getStartDate(),
            it.getEndDate(),
            (int) ChronoUnit.DAYS.between(it.getStartDate(), it.getEndDate()) + 1
        );
    }
}
