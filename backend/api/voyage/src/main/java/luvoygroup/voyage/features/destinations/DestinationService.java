package luvoygroup.voyage.features.destinations;

import luvoygroup.voyage.exception.ResourceNotFoundException;
import luvoygroup.voyage.features.destinations.dto.DestinationDetailDTO;
import luvoygroup.voyage.features.destinations.dto.DestinationSummaryDTO;
import luvoygroup.voyage.repository.DestinationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DestinationService {

    private final DestinationRepository destinationRepository;

    public List<DestinationSummaryDTO> findAllActive() {
        return destinationRepository.findAllByActiveTrue().stream()
            .map(DestinationSummaryDTO::from)
            .toList();
    }

    public List<DestinationSummaryDTO> search(String q) {
        return destinationRepository.searchByQuery(q).stream()
            .map(DestinationSummaryDTO::from)
            .toList();
    }

    public DestinationDetailDTO findBySlug(String slug) {
        return destinationRepository.findBySlug(slug)
            .filter(d -> d.isActive())
            .map(DestinationDetailDTO::from)
            .orElseThrow(ResourceNotFoundException::new);
    }
}
