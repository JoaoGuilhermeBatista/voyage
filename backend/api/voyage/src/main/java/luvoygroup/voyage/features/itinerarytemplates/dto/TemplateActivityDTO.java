package luvoygroup.voyage.features.itinerarytemplates.dto;

import luvoygroup.voyage.features.itinerary.dto.PoiDetailDTO;

public record TemplateActivityDTO(
    String slot,
    int orderInSlot,
    PoiDetailDTO poi
) {}
