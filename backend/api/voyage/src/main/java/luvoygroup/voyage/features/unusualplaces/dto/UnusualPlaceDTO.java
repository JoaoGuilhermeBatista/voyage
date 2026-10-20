package luvoygroup.voyage.features.unusualplaces.dto;

import luvoygroup.voyage.domain.UnusualPlace;

/**
 * {@code locked=true}: usuário sem premium — só {@code title} vem preenchido
 * ({@code description}/{@code category} nulos), convite pra upsell no app.
 * {@code locked=false}: usuário premium — conteúdo completo.
 */
public record UnusualPlaceDTO(String title, String description, String category, boolean locked) {

    public static UnusualPlaceDTO unlocked(UnusualPlace place) {
        return new UnusualPlaceDTO(place.getTitle(), place.getDescription(), place.getCategory(), false);
    }

    public static UnusualPlaceDTO teaser(UnusualPlace place) {
        return new UnusualPlaceDTO(place.getTitle(), null, null, true);
    }
}
