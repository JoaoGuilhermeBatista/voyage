package luvoygroup.voyage.repository;

import luvoygroup.voyage.domain.UnusualPlace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UnusualPlaceRepository extends JpaRepository<UnusualPlace, UUID> {
    List<UnusualPlace> findAllByDestinationIdOrderByOrderIndexAsc(UUID destinationId);
}
