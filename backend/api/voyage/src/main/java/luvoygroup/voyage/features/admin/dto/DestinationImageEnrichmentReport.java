package luvoygroup.voyage.features.admin.dto;

import java.util.List;

/**
 * Relatório de AdminService#enrichDestinationImages — mesmo SHAPE de
 * {@link PoiEnrichmentReport}, mas no nível de destino (não POI): enriquece
 * {@code Destination.images} in-place, nunca mexe em {@code heroImageUrl}.
 */
public record DestinationImageEnrichmentReport(
    int destinationsProcessed,
    int destinationsSkipped,
    int totalImagesAdded,
    List<String> processedSlugs,
    List<String> lowYieldSlugs,
    String errorReason
) {}
