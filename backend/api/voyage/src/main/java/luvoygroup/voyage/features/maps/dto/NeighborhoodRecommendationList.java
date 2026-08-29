package luvoygroup.voyage.features.maps.dto;

import java.util.List;

/**
 * Wrapper de saída estruturada do LLM para
 * {@code LlmService#recommendNeighborhoods} — mesmo padrão de
 * {@code luvoygroup.voyage.features.admin.dto.PoiEnrichmentClassification}.
 * O LLM não deve inventar bairros que não estejam na lista de candidatos
 * fornecida — a responsabilidade de filtrar qualquer nome alucinado é de
 * quem consome esta resposta (ver contrato documentado em
 * {@code LlmService#recommendNeighborhoods}).
 */
public record NeighborhoodRecommendationList(List<NeighborhoodRecommendation> recommendations) {}
