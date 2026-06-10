package luvoygroup.voyage.repository;

import luvoygroup.voyage.domain.ItineraryActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ItineraryActivityRepository extends JpaRepository<ItineraryActivity, UUID> {
}
