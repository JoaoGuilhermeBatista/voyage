package luvoygroup.voyage.features.itinerary.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SubstituteRequest(@NotNull UUID poiId) {}
