package luvoygroup.voyage.repository;

import luvoygroup.voyage.domain.ItineraryTemplateDay;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ItineraryTemplateDayRepository extends JpaRepository<ItineraryTemplateDay, UUID> {
}
