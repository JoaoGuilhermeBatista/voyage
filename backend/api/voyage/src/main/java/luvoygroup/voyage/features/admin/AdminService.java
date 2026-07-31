package luvoygroup.voyage.features.admin;

import luvoygroup.voyage.domain.Destination;
import luvoygroup.voyage.domain.Poi;
import luvoygroup.voyage.domain.enums.BudgetTier;
import luvoygroup.voyage.domain.enums.PoiCategory;
import luvoygroup.voyage.features.admin.dto.*;
import luvoygroup.voyage.features.destinations.dto.DestinationSummaryDTO;
import luvoygroup.voyage.repository.DestinationRepository;
import luvoygroup.voyage.repository.PoiRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdminService {

    private final LlmService llmService;
    private final DestinationRepository destinationRepo;
    private final PoiRepository poiRepo;

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

    public BatchGenerationReport generatePoisBatch() {
        List<Destination> pending = destinationRepo.findAllWithNoPois();
        log.info("Destinos sem POIs: {}", pending.size());

        List<String> processed = new ArrayList<>();
        int totalPois = 0;
        int skipped = 0;

        for (int i = 0; i < pending.size(); i += 5) {
            List<Destination> batch = pending.subList(i, Math.min(i + 5, pending.size()));

            for (Destination destination : batch) {
                try {
                    int count = generateAndSavePoisForDestination(destination);
                    processed.add(destination.getSlug());
                    totalPois += count;
                    log.info("✓ {} — {} POIs gerados", destination.getSlug(), count);
                } catch (Exception e) {
                    log.error("✗ Erro em {} — parando batch: {}", destination.getSlug(), e.getMessage());
                    skipped = pending.size() - processed.size() - 1;
                    return new BatchGenerationReport(
                        processed.size(), skipped, totalPois, processed,
                        destination.getSlug(), e.getMessage()
                    );
                }
            }
        }

        return new BatchGenerationReport(
            processed.size(), 0, totalPois, processed, null, null
        );
    }

    @Transactional
    public int generateAndSavePoisForDestination(Destination destination) {
        PoisWrapper wrapper = llmService.generatePois(destination.getCity(), destination.getCountry());

        if (wrapper == null || wrapper.pois() == null || wrapper.pois().isEmpty())
            throw new IllegalStateException("GPT não retornou POIs para " + destination.getSlug());

        List<Poi> pois = wrapper.pois().stream()
            .map(gp -> mapToPoi(gp, destination))
            .filter(p -> p != null)
            .toList();

        poiRepo.saveAll(pois);
        return pois.size();
    }

    private Poi mapToPoi(GeneratedPoi gp, Destination destination) {
        try {
            PoiCategory category = PoiCategory.valueOf(gp.category().toUpperCase());
            BudgetTier budgetTier = BudgetTier.valueOf(gp.budgetTier().toUpperCase());

            return Poi.builder()
                .destination(destination)
                .title(gp.title())
                .category(category)
                .description(gp.description())
                .address(gp.address())
                .latitude(gp.latitude() != null ? BigDecimal.valueOf(gp.latitude()) : null)
                .longitude(gp.longitude() != null ? BigDecimal.valueOf(gp.longitude()) : null)
                .budgetTier(budgetTier)
                .averageDurationMinutes(gp.averageDurationMinutes())
                .openingTime(parseTime(gp.openingTime()))
                .closingTime(parseTime(gp.closingTime()))
                .tags(gp.tags() != null ? gp.tags() : List.of())
                .active(true)
                .build();
        } catch (Exception e) {
            log.warn("POI ignorado (dados inválidos): {} — {}", gp.title(), e.getMessage());
            return null;
        }
    }

    private LocalTime parseTime(String time) {
        if (time == null || time.isBlank() || time.equalsIgnoreCase("null")) return null;
        try {
            return LocalTime.parse(time);
        } catch (Exception e) {
            return null;
        }
    }
}
