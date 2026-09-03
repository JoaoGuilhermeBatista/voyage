package luvoygroup.voyage.features.admin.dto;

/**
 * Uma mudança de título de POI proposta por {@code LlmService#normalizePoiTitleLanguage}
 * para corrigir inconsistência de idioma dentro do mesmo destino.
 * {@code originalTitle} deve ser sempre um título EXATO (verbatim) já
 * cadastrado para o destino — nunca um título inventado pelo LLM (ver
 * contrato de {@code LlmService#normalizePoiTitleLanguage}).
 */
public record PoiTitleChange(String originalTitle, String normalizedTitle) {}
