package luvoygroup.voyage.features.destinations.dto;

import luvoygroup.voyage.domain.Destination;

import java.util.List;
import java.util.UUID;

public record DestinationSummaryDTO(
    UUID id,
    String slug,
    String name,
    String country,
    String city,
    String tagline,
    String heroImageUrl,
    String signature,
    List<String> tags,
    String type
) {
    public static DestinationSummaryDTO from(Destination d) {
        return new DestinationSummaryDTO(
            d.getId(), d.getSlug(), d.getName(), d.getCountry(), d.getCity(),
            d.getTagline(), d.getHeroImageUrl(), d.getSignature(), d.getTags(),
            d.getType().name()
        );
    }
}
