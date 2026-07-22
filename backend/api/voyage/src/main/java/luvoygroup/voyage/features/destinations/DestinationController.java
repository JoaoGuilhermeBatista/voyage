package luvoygroup.voyage.features.destinations;

import luvoygroup.voyage.features.destinations.dto.DestinationDetailDTO;
import luvoygroup.voyage.features.destinations.dto.DestinationSummaryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/destinations")
@RequiredArgsConstructor
public class DestinationController {

    private final DestinationService destinationService;

    @GetMapping
    public List<DestinationSummaryDTO> listAll(@RequestParam(required = false) String q) {
        if (q != null && !q.isBlank()) {
            return destinationService.search(q);
        }
        return destinationService.findAllActive();
    }

    @GetMapping("/{slug}")
    public DestinationDetailDTO getBySlug(@PathVariable String slug) {
        return destinationService.findBySlug(slug);
    }
}
