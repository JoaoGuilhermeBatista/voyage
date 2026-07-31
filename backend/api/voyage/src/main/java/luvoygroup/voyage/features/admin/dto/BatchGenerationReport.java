package luvoygroup.voyage.features.admin.dto;

import java.util.List;

public record BatchGenerationReport(
    int destinationsProcessed,
    int destinationsSkipped,
    int totalPoisCreated,
    List<String> processedSlugs,
    String stoppedAt,
    String errorReason
) {}
