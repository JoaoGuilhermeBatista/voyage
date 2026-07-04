package luvoygroup.voyage.features.destinations.dto;

import luvoygroup.voyage.domain.Poi;
import luvoygroup.voyage.domain.enums.BudgetTier;
import luvoygroup.voyage.domain.enums.PoiCategory;

import java.math.BigDecimal;
import java.util.UUID;

public record PoiSummaryDTO(
    UUID id,
    String title,
    PoiCategory category,
    BudgetTier budgetTier,
    BigDecimal latitude,
    BigDecimal longitude
) {
    public static PoiSummaryDTO from(Poi p) {
        return new PoiSummaryDTO(
            p.getId(), p.getTitle(), p.getCategory(),
            p.getBudgetTier(), p.getLatitude(), p.getLongitude()
        );
    }
}
