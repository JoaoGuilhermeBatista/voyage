package luvoygroup.voyage.features.destinations.dto;

import luvoygroup.voyage.domain.DestinationSection;
import luvoygroup.voyage.domain.enums.SectionType;

import java.util.List;

public record DestinationSectionDTO(
    SectionType sectionType,
    Integer orderIndex,
    List<SectionItemDTO> content
) {
    public static DestinationSectionDTO from(DestinationSection s) {
        List<SectionItemDTO> items = s.getContent().stream()
            .map(i -> new SectionItemDTO(i.getName(), i.getDesc()))
            .toList();
        return new DestinationSectionDTO(s.getSectionType(), s.getOrderIndex(), items);
    }
}
