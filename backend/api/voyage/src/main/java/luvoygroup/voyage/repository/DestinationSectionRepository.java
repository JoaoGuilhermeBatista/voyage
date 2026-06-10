package luvoygroup.voyage.repository;

import luvoygroup.voyage.domain.DestinationSection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DestinationSectionRepository extends JpaRepository<DestinationSection, UUID> {
    List<DestinationSection> findAllByDestinationIdOrderByOrderIndexAsc(UUID destinationId);
}
