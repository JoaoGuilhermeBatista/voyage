package luvoygroup.voyage.features.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record GenerateDestinationRequest(
    @NotBlank String city,
    @NotBlank String country,
    @NotBlank String slug
) {}
