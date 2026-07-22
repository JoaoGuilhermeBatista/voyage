package luvoygroup.voyage.repository;

import luvoygroup.voyage.domain.Destination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DestinationRepository extends JpaRepository<Destination, UUID> {
    List<Destination> findAllByActiveTrue();
    Optional<Destination> findBySlug(String slug);

    @Query("""
        SELECT d FROM Destination d
        WHERE d.active = true
        AND (LOWER(d.name) LIKE LOWER(CONCAT('%', :q, '%'))
          OR LOWER(d.country) LIKE LOWER(CONCAT('%', :q, '%'))
          OR LOWER(d.city) LIKE LOWER(CONCAT('%', :q, '%')))
        """)
    List<Destination> searchByQuery(@Param("q") String q);
}
