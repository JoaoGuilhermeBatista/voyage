package luvoygroup.voyage.repository;

import luvoygroup.voyage.domain.Itinerary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ItineraryRepository extends JpaRepository<Itinerary, UUID> {
    List<Itinerary> findAllByUserIdAndDeletedAtIsNull(UUID userId);
    Optional<Itinerary> findByIdAndDeletedAtIsNull(UUID id);
}
