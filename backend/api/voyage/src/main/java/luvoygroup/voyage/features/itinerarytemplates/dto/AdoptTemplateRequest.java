package luvoygroup.voyage.features.itinerarytemplates.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record AdoptTemplateRequest(
    @NotNull LocalDate startDate
) {}
