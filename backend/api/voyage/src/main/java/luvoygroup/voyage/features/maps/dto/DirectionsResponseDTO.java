package luvoygroup.voyage.features.maps.dto;

/**
 * Resposta do proxy de rota (Google Directions API).
 *
 * Quando a chave do Google Maps não está configurada no backend,
 * {@code configured} vem {@code false} e os demais campos vêm nulos —
 * o app deve esconder a UI de rota graciosamente nesse caso (mesmo
 * espírito do fallback de {@code CurrencyRatesDTO}).
 */
public record DirectionsResponseDTO(
    boolean configured,
    Double distanceMeters,
    Integer durationSeconds,
    String distanceText,
    String durationText
) {}
