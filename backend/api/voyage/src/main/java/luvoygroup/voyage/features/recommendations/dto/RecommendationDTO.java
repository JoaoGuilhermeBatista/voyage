package luvoygroup.voyage.features.recommendations.dto;

import luvoygroup.voyage.domain.Destination;

import java.util.List;
import java.util.UUID;

public record RecommendationDTO(
    UUID id,
    String slug,
    String name,
    String country,
    String city,
    String tagline,
    String heroImageUrl,
    String signature,
    List<String> tags,
    int matchScore
) {
    public static RecommendationDTO from(Destination d, int score) {
        return new RecommendationDTO(
            d.getId(), d.getSlug(), d.getName(), d.getCountry(), d.getCity(),
            d.getTagline(), d.getHeroImageUrl(), d.getSignature(), d.getTags(), score
        );
    }
}
