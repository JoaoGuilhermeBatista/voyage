package luvoygroup.voyage.features.admin.dto;

/**
 * Relatório de AdminService#generatePoisForExistingDestination — diferente de
 * generatePoisBatch (que só processa destinos com ZERO POIs), esse endpoint
 * ADICIONA POIs novos a um destino que já tem POIs (não sobrescreve nem
 * deleta os existentes), usado para "aprofundar" destinos secundários que
 * ficaram com poucos POIs na geração inicial.
 */
public record PoiTopUpReport(
    String destinationSlug,
    int poisGenerated
) {}
