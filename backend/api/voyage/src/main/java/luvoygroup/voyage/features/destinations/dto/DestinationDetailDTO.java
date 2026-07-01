package luvoygroup.voyage.features.destinations.dto;

import luvoygroup.voyage.domain.Destination;

import java.util.List;
import java.util.UUID;

public record DestinationDetailDTO(
    UUID id,
    String slug,
    String name,
    String country,
    String city,
    String tagline,
    String description,
    String heroImageUrl,
    String signature,
    List<String> tags,
    List<DestinationSectionDTO> sections,
    List<PoiSummaryDTO> pois
) {
    public static DestinationDetailDTO from(Destination d) {
        List<DestinationSectionDTO> sections = d.getSections().stream()
            .map(DestinationSectionDTO::from)
            .toList();
        List<PoiSummaryDTO> pois = d.getPois().stream()
            .filter(p -> p.isActive())
            .map(PoiSummaryDTO::from)
            .toList();
        return new DestinationDetailDTO(
            d.getId(), d.getSlug(), d.getName(), d.getCountry(), d.getCity(),
            d.getTagline(), d.getDescription(), d.getHeroImageUrl(), d.getSignature(),
            d.getTags(), sections, pois
        );
    }
}
