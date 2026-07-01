package luvoygroup.voyage.features.favorites.dto;

import luvoygroup.voyage.domain.enums.PoiCategory;

import java.util.UUID;

public record PoiFavoriteDTO(UUID id, String title, PoiCategory category) {}
