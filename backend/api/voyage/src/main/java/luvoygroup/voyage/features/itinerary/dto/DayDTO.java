package luvoygroup.voyage.features.itinerary.dto;

import java.time.LocalDate;
import java.util.List;

public record DayDTO(
    Integer dayNumber,
    LocalDate date,
    List<ActivityDTO> activities
) {}
