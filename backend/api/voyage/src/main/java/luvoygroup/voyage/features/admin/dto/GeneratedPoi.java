package luvoygroup.voyage.features.admin.dto;

import java.util.List;

public record GeneratedPoi(
    String title,
    String category,
    String description,
    String address,
    Double latitude,
    Double longitude,
    String budgetTier,
    Integer averageDurationMinutes,
    String openingTime,
    String closingTime,
    List<String> tags,
    Boolean mustSee
) {}
