package luvoygroup.voyage.features.itinerary.dto;

import luvoygroup.voyage.domain.enums.ItinerarySlot;

import java.util.UUID;

public record ActivityDTO(
    UUID id,
    ItinerarySlot slot,
    Integer orderInSlot,
    PoiDetailDTO poi
) {}
