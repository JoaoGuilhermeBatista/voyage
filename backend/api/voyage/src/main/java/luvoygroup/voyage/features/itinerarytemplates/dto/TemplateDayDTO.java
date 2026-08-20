package luvoygroup.voyage.features.itinerarytemplates.dto;

import java.util.List;

public record TemplateDayDTO(
    int dayNumber,
    List<TemplateActivityDTO> activities
) {}
