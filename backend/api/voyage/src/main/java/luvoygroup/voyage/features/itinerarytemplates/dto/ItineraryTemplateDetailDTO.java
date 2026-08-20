package luvoygroup.voyage.features.itinerarytemplates.dto;

import luvoygroup.voyage.domain.enums.BudgetTier;

import java.util.List;
import java.util.UUID;

public record ItineraryTemplateDetailDTO(
    UUID id,
    String title,
    String description,
    int durationDays,
    BudgetTier budgetTier,
    UUID destinationId,
    String destinationName,
    List<TemplateDayDTO> days
) {}
