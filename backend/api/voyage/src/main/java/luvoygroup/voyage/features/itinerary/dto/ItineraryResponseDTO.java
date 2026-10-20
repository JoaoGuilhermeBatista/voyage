package luvoygroup.voyage.features.itinerary.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ItineraryResponseDTO(
    UUID id,
    String title,
    UUID destinationId,
    String destinationName,
    String destinationHeroImageUrl,
    String destinationCountry,
    LocalDate startDate,
    LocalDate endDate,
    List<DayDTO> days
) {}
