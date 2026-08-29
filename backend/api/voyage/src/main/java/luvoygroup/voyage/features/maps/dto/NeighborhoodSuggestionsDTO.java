package luvoygroup.voyage.features.maps.dto;

import java.util.List;

/**
 * Resposta do proxy de sugestão de bairro para hospedagem (Google Places
 * Nearby Search + geocodificação reversa + recomendação por LLM).
 *
 * Quando a chave do Google Maps não está configurada no backend,
 * {@code configured} vem {@code false} e {@code neighborhoods} vem vazia —
 * o app deve esconder a UI de sugestão de bairro graciosamente nesse caso
 * (mesmo espírito do fallback de {@code DirectionsResponseDTO}).
 */
public record NeighborhoodSuggestionsDTO(
    boolean configured,
    List<NeighborhoodRecommendation> neighborhoods
) {}
