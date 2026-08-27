package luvoygroup.voyage.features.itinerary.dto;

import jakarta.validation.constraints.NotNull;
import luvoygroup.voyage.domain.enums.BudgetTier;
import luvoygroup.voyage.domain.enums.GroupType;
import luvoygroup.voyage.domain.enums.TravelRhythm;
import luvoygroup.voyage.domain.enums.TripVibe;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ItineraryRequest(
    @NotNull UUID destinationId,
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate,
    String stayArea,
    TripVibe tripVibe,
    GroupType companions,
    BudgetTier tripBudget,
    List<String> tripInterests,
    TravelRhythm tripRhythm
) {}
