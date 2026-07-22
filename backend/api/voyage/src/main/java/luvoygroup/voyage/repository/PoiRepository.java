package luvoygroup.voyage.repository;

import luvoygroup.voyage.domain.Poi;
import luvoygroup.voyage.domain.enums.BudgetTier;
import luvoygroup.voyage.domain.enums.PoiCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PoiRepository extends JpaRepository<Poi, UUID> {

    @Query("SELECT p FROM Poi p WHERE p.destination.id = :destinationId AND p.budgetTier IN :budgetTiers AND p.active = true")
    List<Poi> findActiveByDestinationAndBudget(
        @Param("destinationId") UUID destinationId,
        @Param("budgetTiers") List<BudgetTier> budgetTiers
    );

    List<Poi> findAllByDestinationIdAndActiveTrue(UUID destinationId);

    @Query("""
        SELECT p FROM Poi p
        WHERE p.destination.id = :destinationId
          AND p.category = :category
          AND p.active = true
          AND p.id NOT IN :excludeIds
        """)
    List<Poi> findAlternatives(
        @Param("destinationId") UUID destinationId,
        @Param("category") PoiCategory category,
        @Param("excludeIds") List<UUID> excludeIds
    );
}
