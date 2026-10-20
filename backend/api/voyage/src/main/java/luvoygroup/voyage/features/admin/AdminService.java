package luvoygroup.voyage.features.admin;

import luvoygroup.voyage.domain.Destination;
import luvoygroup.voyage.domain.Poi;
import luvoygroup.voyage.domain.enums.BudgetTier;
import luvoygroup.voyage.domain.enums.PoiCategory;
import luvoygroup.voyage.exception.ResourceNotFoundException;
import luvoygroup.voyage.features.admin.dto.*;
import luvoygroup.voyage.features.destinations.dto.DestinationSummaryDTO;
import luvoygroup.voyage.features.itinerarytemplates.ItineraryTemplateService;
import luvoygroup.voyage.features.itinerarytemplates.dto.GenerateTemplateRequest;
import luvoygroup.voyage.features.itinerarytemplates.dto.ItineraryTemplateDTO;
import luvoygroup.voyage.features.maps.PlacesService;
import luvoygroup.voyage.features.unusualplaces.UnusualPlaceService;
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
    private final ItineraryTemplateService itineraryTemplateService;
    private final UnusualPlaceService unusualPlaceService;
    private final PlacesService placesService;

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
        return saveGeneratedPois(wrapper, destination);
    }

    @Transactional
    public int generateAndSavePoisForDestination(Destination destination, String focusCategory, List<String> existingTitles) {
        PoisWrapper wrapper = llmService.generatePois(
            destination.getCity(), destination.getCountry(), focusCategory, existingTitles
        );
        return saveGeneratedPois(wrapper, destination);
    }

    private int saveGeneratedPois(PoisWrapper wrapper, Destination destination) {
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
                .mustSee(Boolean.TRUE.equals(gp.mustSee()))
                .catholicChurch(Boolean.TRUE.equals(gp.catholicChurch()))
                .fullDayActivity(Boolean.TRUE.equals(gp.fullDayActivity()))
                .build();
        } catch (Exception e) {
            log.warn("POI ignorado (dados inválidos): {} — {}", gp.title(), e.getMessage());
            return null;
        }
    }

    public ItineraryTemplateDTO generateItineraryTemplate(String slug, GenerateTemplateRequest request) {
        return itineraryTemplateService.generateForDestination(slug, request.durationDays(), request.budgetTier());
    }

    public int generateUnusualPlaces(String slug) {
        return unusualPlaceService.generateForDestination(slug);
    }

    /**
     * Classifica retroativamente, IN PLACE (mesmo id, sem deletar/recriar —
     * POIs são referenciados por FK em atividades de itinerário, templates e
     * favoritos), quais POIs já existentes são igrejas católicas. Erro em um
     * destino não interrompe os demais (cada destino é independente,
     * diferente de generatePoisBatch, que para no primeiro erro).
     */
    public PoiEnrichmentReport enrichExistingPoisWithCatholicChurchFlag() {
        List<Destination> destinations = destinationRepo.findAllByActiveTrue();
        log.info("Destinos ativos a verificar para enriquecimento de igreja católica: {}", destinations.size());

        List<String> processed = new ArrayList<>();
        int totalEnriched = 0;
        int skipped = 0;

        for (Destination destination : destinations) {
            List<Poi> pois = poiRepo.findAllByDestinationIdAndActiveTrue(destination.getId());
            if (pois.isEmpty()) {
                skipped++;
                continue;
            }

            try {
                List<String> titles = pois.stream().map(Poi::getTitle).toList();
                CatholicChurchClassification classification =
                    llmService.classifyCatholicChurches(destination.getCity(), destination.getCountry(), titles);

                int enrichedForDestination = 0;
                if (classification != null && classification.catholicChurchTitles() != null) {
                    for (String returnedTitle : classification.catholicChurchTitles()) {
                        Poi matched = pois.stream()
                            .filter(p -> p.getTitle().equals(returnedTitle))
                            .findFirst()
                            .orElse(null);

                        if (matched == null) {
                            log.warn("Título alucinado pelo LLM ignorado (não pertence ao catálogo do destino {}): {}",
                                destination.getSlug(), returnedTitle);
                            continue;
                        }

                        matched.setCatholicChurch(true);
                        poiRepo.save(matched);
                        enrichedForDestination++;
                    }
                }

                processed.add(destination.getSlug());
                totalEnriched += enrichedForDestination;
                log.info("✓ {} — {} POIs marcados como igreja católica", destination.getSlug(), enrichedForDestination);
            } catch (Exception e) {
                log.error("✗ Erro em {} — pulando destino: {}", destination.getSlug(), e.getMessage());
                skipped++;
            }
        }

        return new PoiEnrichmentReport(
            processed.size(), skipped, totalEnriched, processed, null, null
        );
    }

    /**
     * Classifica retroativamente, IN PLACE (mesmo id, sem deletar/recriar —
     * POIs são referenciados por FK em atividades de itinerário, templates e
     * favoritos), quais POIs já existentes são "imperdíveis" (mustSee) e/ou
     * deveriam ser recategorizados como LANDMARK. Destinos que já possuem
     * algum POI ativo com mustSee=true são pulados (operação idempotente),
     * assim como destinos sem POIs ativos. Erro em um destino não interrompe
     * os demais (mesmo padrão de enrichExistingPoisWithCatholicChurchFlag).
     */
    public PoiEnrichmentReport enrichExistingPoisWithMustSeeAndLandmark() {
        List<Destination> destinations = destinationRepo.findAllByActiveTrue();
        log.info("Destinos ativos a verificar para enriquecimento de mustSee/LANDMARK: {}", destinations.size());

        List<String> processed = new ArrayList<>();
        int totalEnriched = 0;
        int skipped = 0;

        for (Destination destination : destinations) {
            List<Poi> pois = poiRepo.findAllByDestinationIdAndActiveTrue(destination.getId());
            if (pois.isEmpty() || pois.stream().anyMatch(Poi::isMustSee)) {
                skipped++;
                continue;
            }

            try {
                PoiEnrichmentClassification classification =
                    llmService.classifyMustSeeAndLandmarks(destination.getCity(), destination.getCountry(), pois);

                List<String> mustSeeTitles = classification != null && classification.mustSeeTitles() != null
                    ? classification.mustSeeTitles() : List.of();
                List<String> landmarkTitles = classification != null && classification.landmarkTitles() != null
                    ? classification.landmarkTitles() : List.of();

                for (String hallucinated : mustSeeTitles) {
                    if (pois.stream().noneMatch(p -> p.getTitle().equals(hallucinated))) {
                        log.warn("Título alucinado pelo LLM ignorado (mustSee, não pertence ao catálogo do destino {}): {}",
                            destination.getSlug(), hallucinated);
                    }
                }
                for (String hallucinated : landmarkTitles) {
                    if (pois.stream().noneMatch(p -> p.getTitle().equals(hallucinated))) {
                        log.warn("Título alucinado pelo LLM ignorado (landmark, não pertence ao catálogo do destino {}): {}",
                            destination.getSlug(), hallucinated);
                    }
                }

                int enrichedForDestination = 0;
                for (Poi poi : pois) {
                    boolean changed = false;

                    if (mustSeeTitles.contains(poi.getTitle())) {
                        poi.setMustSee(true);
                        changed = true;
                    }
                    if (landmarkTitles.contains(poi.getTitle())) {
                        poi.setCategory(PoiCategory.LANDMARK);
                        changed = true;
                    }

                    if (changed) {
                        poiRepo.save(poi);
                        enrichedForDestination++;
                    }
                }

                processed.add(destination.getSlug());
                totalEnriched += enrichedForDestination;
                log.info("✓ {} — {} POIs marcados como mustSee/LANDMARK", destination.getSlug(), enrichedForDestination);
            } catch (Exception e) {
                log.error("✗ Erro em {} — pulando destino: {}", destination.getSlug(), e.getMessage());
                skipped++;
            }
        }

        return new PoiEnrichmentReport(
            processed.size(), skipped, totalEnriched, processed, null, null
        );
    }

    /**
     * Popula retroativamente, IN PLACE (mesmo id, sem deletar/recriar —
     * POIs são referenciados por FK em atividades de itinerário, templates
     * de itinerário e favoritos), {@code Poi.images} via
     * {@link PlacesService#fetchPhotoUrl}, para os POIs que ainda não têm
     * nenhuma foto. Destino sem nenhum POI sem foto é pulado (operação
     * idempotente). Erro em um destino não interrompe os demais (mesmo
     * padrão de enrichExistingPoisWithCatholicChurchFlag).
     */
    public PoiEnrichmentReport enrichExistingPoisWithImages() {
        List<Destination> destinations = destinationRepo.findAllByActiveTrue();
        log.info("Destinos ativos a verificar para enriquecimento de imagens: {}", destinations.size());

        List<String> processed = new ArrayList<>();
        int totalEnriched = 0;
        int skipped = 0;

        for (Destination destination : destinations) {
            List<Poi> poisWithoutImages = poiRepo.findAllByDestinationIdAndActiveTrue(destination.getId()).stream()
                .filter(p -> p.getImages().isEmpty())
                .toList();
            if (poisWithoutImages.isEmpty()) {
                skipped++;
                continue;
            }

            int enrichedForDestination = 0;
            try {
                for (Poi poi : poisWithoutImages) {
                    var photoUrl = placesService.fetchPhotoUrl(poi.getTitle(), destination.getCity());
                    if (photoUrl.isPresent()) {
                        poi.getImages().add(photoUrl.get());
                        poiRepo.save(poi);
                        enrichedForDestination++;
                        totalEnriched++;
                    }
                }

                processed.add(destination.getSlug());
                log.info("✓ {} — {} POIs enriquecidos com imagem", destination.getSlug(), enrichedForDestination);
            } catch (Exception e) {
                log.error("✗ Erro em {} — {} POIs ja enriquecidos antes da falha: {}",
                    destination.getSlug(), enrichedForDestination, e.getMessage());
                skipped++;
            }
        }

        return new PoiEnrichmentReport(
            processed.size(), skipped, totalEnriched, processed, null, null
        );
    }

    /**
     * Normaliza retroativamente, IN PLACE (mesmo id, sem deletar/recriar —
     * POIs são referenciados por FK em atividades de itinerário, templates
     * de itinerário e favoritos), o idioma do título de POIs já existentes:
     * dentro do MESMO destino, alguns títulos estão traduzidos pro
     * português e outros não, sem critério (achado revisando o catálogo em
     * 2026-09, ver luvoy-pendencias.md). Diferente de
     * enrichExistingPoisWithMustSeeAndLandmark (que pula destino que já tem
     * mustSee), aqui TODO destino ativo com POIs é processado — o problema
     * de idioma já existe no catálogo inteiro, não é condicional. Erro em
     * um destino não interrompe os demais (mesmo padrão de
     * enrichExistingPoisWithCatholicChurchFlag).
     */
    public PoiEnrichmentReport enrichExistingPoisWithNormalizedTitleLanguage() {
        List<Destination> destinations = destinationRepo.findAllByActiveTrue();
        log.info("Destinos ativos a verificar para normalização de idioma de título: {}", destinations.size());

        List<String> processed = new ArrayList<>();
        int totalEnriched = 0;
        int skipped = 0;

        for (Destination destination : destinations) {
            List<Poi> pois = poiRepo.findAllByDestinationIdAndActiveTrue(destination.getId());
            if (pois.isEmpty()) {
                skipped++;
                continue;
            }

            try {
                List<String> titles = pois.stream().map(Poi::getTitle).toList();
                PoiTitleNormalization normalization =
                    llmService.normalizePoiTitleLanguage(destination.getCity(), destination.getCountry(), titles);

                int enrichedForDestination = 0;
                if (normalization != null && normalization.changes() != null) {
                    for (PoiTitleChange change : normalization.changes()) {
                        Poi matched = pois.stream()
                            .filter(p -> p.getTitle().equals(change.originalTitle()))
                            .findFirst()
                            .orElse(null);

                        if (matched == null) {
                            log.warn("Título alucinado pelo LLM ignorado (normalização de idioma, não pertence ao catálogo do destino {}): {}",
                                destination.getSlug(), change.originalTitle());
                            continue;
                        }
                        if (change.normalizedTitle() == null || change.normalizedTitle().isBlank()) {
                            log.warn("normalizedTitle vazio pelo LLM ignorado para '{}' em {}",
                                change.originalTitle(), destination.getSlug());
                            continue;
                        }

                        log.info("  '{}' → '{}'", matched.getTitle(), change.normalizedTitle());
                        matched.setTitle(change.normalizedTitle());
                        poiRepo.save(matched);
                        enrichedForDestination++;
                    }
                }

                processed.add(destination.getSlug());
                totalEnriched += enrichedForDestination;
                log.info("✓ {} — {} títulos normalizados", destination.getSlug(), enrichedForDestination);
            } catch (Exception e) {
                log.error("✗ Erro em {} — pulando destino: {}", destination.getSlug(), e.getMessage());
                skipped++;
            }
        }

        return new PoiEnrichmentReport(
            processed.size(), skipped, totalEnriched, processed, null, null
        );
    }

    /**
     * Gera e ADICIONA POIs novos a um destino que já existe (diferente de
     * generatePoisBatch, que só processa destinos com ZERO POIs) — usado
     * para aprofundar destinos secundários que ficaram com poucos POIs na
     * geração inicial. Não sobrescreve nem deleta os POIs já existentes.
     */
    @Transactional
    public PoiTopUpReport generatePoisForExistingDestination(String slug) {
        return generatePoisForExistingDestination(slug, (PoiCategory) null);
    }

    /**
     * Mesmo comportamento acima, mas permite focar o reforço numa categoria
     * específica (ex: RESTAURANT, NIGHTLIFE) quando o destino está com
     * cobertura fraca nela — pede pro LLM priorizar essa categoria e evita
     * repetir os POIs já cadastrados (mesmo padrão de contexto negativo
     * usado em UnusualPlaceService).
     */
    @Transactional
    public PoiTopUpReport generatePoisForExistingDestination(String slug, PoiCategory focusCategory) {
        Destination destination = destinationRepo.findBySlug(slug)
            .orElseThrow(ResourceNotFoundException::new);

        List<String> existingTitles = poiRepo.findAllByDestinationIdAndActiveTrue(destination.getId())
            .stream().map(Poi::getTitle).toList();

        int count = generateAndSavePoisForDestination(
            destination,
            focusCategory == null ? null : focusCategory.name(),
            existingTitles
        );
        return new PoiTopUpReport(slug, count);
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
