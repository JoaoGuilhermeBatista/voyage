package luvoygroup.voyage.features.admin.dto;

import java.util.List;

/**
 * Wrapper de saída estruturada do LLM para
 * {@code LlmService#normalizePoiTitleLanguage} — mesmo padrão de
 * {@code PoiEnrichmentClassification} e
 * {@code luvoygroup.voyage.features.maps.dto.NeighborhoodRecommendationList}.
 * Contém SOMENTE os títulos que precisam mudar — um título que já está
 * correto (traduzido ou mantido no original, conforme o critério editorial)
 * não aparece em {@code changes}.
 */
public record PoiTitleNormalization(List<PoiTitleChange> changes) {}
