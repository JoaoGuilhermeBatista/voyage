package luvoygroup.voyage.repository;

import luvoygroup.voyage.domain.ItineraryDay;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ItineraryDayRepository extends JpaRepository<ItineraryDay, UUID> {
}
