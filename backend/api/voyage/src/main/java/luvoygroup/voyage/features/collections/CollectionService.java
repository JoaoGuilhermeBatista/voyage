package luvoygroup.voyage.features.collections;

import luvoygroup.voyage.exception.ResourceNotFoundException;
import luvoygroup.voyage.features.collections.dto.CollectionDTO;
import luvoygroup.voyage.repository.CollectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CollectionService {

    private final CollectionRepository collectionRepository;

    public List<CollectionDTO> findAll() {
        return collectionRepository.findAllByOrderByOrderIndexAsc().stream()
            .map(CollectionDTO::from)
            .toList();
    }

    public CollectionDTO findBySlug(String slug) {
        return collectionRepository.findBySlug(slug)
            .map(CollectionDTO::from)
            .orElseThrow(ResourceNotFoundException::new);
    }
}
