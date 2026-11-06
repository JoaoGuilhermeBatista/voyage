package luvoygroup.voyage.repository;

import luvoygroup.voyage.domain.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FavoriteRepository extends JpaRepository<Favorite, UUID> {
    List<Favorite> findAllByUserId(UUID userId);
}
