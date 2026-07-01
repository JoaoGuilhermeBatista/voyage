package luvoygroup.voyage.features.collections.dto;

import luvoygroup.voyage.domain.Collection;
import luvoygroup.voyage.features.destinations.dto.DestinationSummaryDTO;

import java.util.List;
import java.util.UUID;

public record CollectionDTO(
    UUID id,
    String slug,
    String title,
    String italic,
    String eyebrow,
    String description,
    Integer orderIndex,
    List<DestinationSummaryDTO> destinations
) {
    public static CollectionDTO from(Collection c) {
        List<DestinationSummaryDTO> dests = c.getDestinations().stream()
            .filter(d -> d.isActive())
            .map(DestinationSummaryDTO::from)
            .toList();
        return new CollectionDTO(
            c.getId(), c.getSlug(), c.getTitle(), c.getItalic(),
            c.getEyebrow(), c.getDescription(), c.getOrderIndex(), dests
        );
    }
}
