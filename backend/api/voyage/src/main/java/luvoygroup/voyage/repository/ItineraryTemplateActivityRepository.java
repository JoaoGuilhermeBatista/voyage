package luvoygroup.voyage.repository;

import luvoygroup.voyage.domain.ItineraryTemplateActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ItineraryTemplateActivityRepository extends JpaRepository<ItineraryTemplateActivity, UUID> {
}
