package luvoygroup.voyage.features.collections;

import luvoygroup.voyage.features.collections.dto.CollectionDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/collections")
@RequiredArgsConstructor
public class CollectionController {

    private final CollectionService collectionService;

    @GetMapping
    public List<CollectionDTO> listAll() {
        return collectionService.findAll();
    }

    @GetMapping("/{slug}")
    public CollectionDTO getBySlug(@PathVariable String slug) {
        return collectionService.findBySlug(slug);
    }
}
