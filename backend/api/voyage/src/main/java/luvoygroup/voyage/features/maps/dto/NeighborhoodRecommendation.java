package luvoygroup.voyage.features.maps.dto;

/**
 * Um bairro recomendado para hospedagem, com uma frase curta (1 linha, em
 * português) descrevendo as vantagens de se hospedar ali para a faixa de
 * budget consultada. {@code name} é sempre um dos candidatos reais
 * descobertos via {@code PlacesService#discoverRealNeighborhoods} — nunca um
 * nome inventado pelo LLM (ver contrato de
 * {@code LlmService#recommendNeighborhoods}).
 */
public record NeighborhoodRecommendation(String name, String advantage) {}
