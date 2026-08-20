package luvoygroup.voyage.features.maps.dto;

/**
 * Resposta do proxy de embed (Google Maps Embed API).
 *
 * Quando a chave do Google Maps não está configurada no backend,
 * {@code configured} vem {@code false} e {@code embedUrl} vem nulo — a
 * chave nunca chega ao app Flutter em nenhum dos dois casos, pois é o
 * backend que monta a URL final.
 */
public record EmbedUrlResponseDTO(
    boolean configured,
    String embedUrl
) {}
