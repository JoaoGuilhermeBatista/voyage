package luvoygroup.voyage.repository;

import luvoygroup.voyage.domain.ItineraryTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ItineraryTemplateRepository extends JpaRepository<ItineraryTemplate, UUID> {
    List<ItineraryTemplate> findAllByDestinationId(UUID destinationId);
}
