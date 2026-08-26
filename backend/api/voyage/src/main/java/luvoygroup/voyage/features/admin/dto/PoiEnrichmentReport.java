package luvoygroup.voyage.features.admin.dto;

import java.util.List;

/**
 * Relatório de AdminService#enrichExistingPoisWithCatholicChurchFlag.
 *
 * Mesmo SHAPE de BatchGenerationReport (usado por generatePoisBatch), mas
 * com nomes de campo próprios para o que essa operação realmente faz:
 * ENRIQUECER POIs já existentes in-place (nunca cria ou deleta POIs), então
 * "totalPoisCreated" de BatchGenerationReport não faz sentido semântico
 * aqui — foi trocado por totalPoisEnriched (quantos POIs tiveram
 * catholicChurch alterado para true).
 */
public record PoiEnrichmentReport(
    int destinationsProcessed,
    int destinationsSkipped,
    int totalPoisEnriched,
    List<String> processedSlugs,
    String stoppedAt,
    String errorReason
) {}
