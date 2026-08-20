package luvoygroup.voyage.features.itinerarytemplates.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import luvoygroup.voyage.domain.enums.BudgetTier;

public record GenerateTemplateRequest(
    @Positive int durationDays,
    @NotNull BudgetTier budgetTier
) {}
