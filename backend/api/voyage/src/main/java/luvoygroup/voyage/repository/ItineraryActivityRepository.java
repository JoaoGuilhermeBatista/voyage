package luvoygroup.voyage.repository;

import luvoygroup.voyage.domain.ItineraryActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ItineraryActivityRepository extends JpaRepository<ItineraryActivity, UUID> {

    Optional<ItineraryActivity> findByIdAndItineraryDayItineraryId(UUID activityId, UUID itineraryId);

    @Query("SELECT a.poi.id FROM ItineraryActivity a WHERE a.itineraryDay.itinerary.id = :itineraryId")
    List<UUID> findAllPoiIdsByItineraryId(@Param("itineraryId") UUID itineraryId);
}
