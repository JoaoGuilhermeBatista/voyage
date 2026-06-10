package luvoygroup.voyage.repository;

import luvoygroup.voyage.domain.Collection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CollectionRepository extends JpaRepository<Collection, UUID> {
    List<Collection> findAllByOrderByOrderIndexAsc();
    Optional<Collection> findBySlug(String slug);
}
