package luvoygroup.voyage.features.itinerary.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record ItineraryRequest(
    @NotNull UUID destinationId,
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate
) {}
