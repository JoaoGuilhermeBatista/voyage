package luvoygroup.voyage.features.itinerary.dto;

import luvoygroup.voyage.domain.Poi;
import luvoygroup.voyage.domain.enums.BudgetTier;
import luvoygroup.voyage.domain.enums.PoiCategory;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PoiDetailDTO(
    UUID id,
    String title,
    PoiCategory category,
    String description,
    String address,
    BigDecimal latitude,
    BigDecimal longitude,
    List<String> images,
    BudgetTier budgetTier,
    Integer averageDurationMinutes
) {
    public static PoiDetailDTO from(Poi p) {
        return new PoiDetailDTO(
            p.getId(),
            p.getTitle(),
            p.getCategory(),
            p.getDescription(),
            p.getAddress(),
            p.getLatitude(),
            p.getLongitude(),
            p.getImages(),
            p.getBudgetTier(),
            p.getAverageDurationMinutes()
        );
    }
}
