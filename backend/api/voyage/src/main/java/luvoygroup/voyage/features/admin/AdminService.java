package luvoygroup.voyage.features.admin;

import luvoygroup.voyage.domain.Destination;
import luvoygroup.voyage.exception.ResourceNotFoundException;
import luvoygroup.voyage.features.admin.dto.GenerateDestinationRequest;
import luvoygroup.voyage.features.admin.dto.GeneratedContent;
import luvoygroup.voyage.features.destinations.dto.DestinationSummaryDTO;
import luvoygroup.voyage.repository.DestinationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final LlmService llmService;
    private final DestinationRepository destinationRepo;

    @Transactional
    public DestinationSummaryDTO generateAndSaveDestination(GenerateDestinationRequest request) {
        if (destinationRepo.findBySlug(request.slug()).isPresent())
            throw new IllegalArgumentException("Slug '%s' já existe".formatted(request.slug()));

        GeneratedContent content = llmService.generateDestinationContent(request.city(), request.country());

        Destination destination = Destination.builder()
            .slug(request.slug())
            .name(content.name())
            .country(request.country())
            .city(request.city())
            .tagline(content.tagline())
            .description(content.description())
            .signature(content.signature())
            .tags(content.tags())
            .active(true)
            .build();

        return DestinationSummaryDTO.from(destinationRepo.save(destination));
    }
}
