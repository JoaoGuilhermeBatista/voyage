package luvoygroup.voyage.features.itinerarytemplates.dto;

import luvoygroup.voyage.domain.enums.BudgetTier;

import java.util.UUID;

public record ItineraryTemplateSummaryDTO(
    UUID id,
    String title,
    String description,
    int durationDays,
    BudgetTier budgetTier
) {}
