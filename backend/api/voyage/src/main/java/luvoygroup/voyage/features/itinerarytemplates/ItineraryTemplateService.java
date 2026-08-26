package luvoygroup.voyage.features.itinerarytemplates;

import luvoygroup.voyage.domain.*;
import luvoygroup.voyage.domain.enums.BudgetTier;
import luvoygroup.voyage.domain.enums.ItinerarySlot;
import luvoygroup.voyage.exception.ResourceNotFoundException;
import luvoygroup.voyage.features.itinerary.dto.ActivityDTO;
import luvoygroup.voyage.features.itinerary.dto.DayDTO;
import luvoygroup.voyage.features.itinerary.dto.ItineraryResponseDTO;
import luvoygroup.voyage.features.itinerary.dto.PoiDetailDTO;
import luvoygroup.voyage.features.itinerarytemplates.dto.GeneratedTemplateActivity;
import luvoygroup.voyage.features.itinerarytemplates.dto.GeneratedTemplateContent;
import luvoygroup.voyage.features.itinerarytemplates.dto.ItineraryTemplateDTO;
import luvoygroup.voyage.features.itinerarytemplates.dto.ItineraryTemplateDetailDTO;
import luvoygroup.voyage.features.itinerarytemplates.dto.ItineraryTemplateSummaryDTO;
import luvoygroup.voyage.features.itinerarytemplates.dto.TemplateActivityDTO;
import luvoygroup.voyage.features.itinerarytemplates.dto.TemplateDayDTO;
import luvoygroup.voyage.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Templates de roteiro curados por destino: geração via LLM a partir do
 * catálogo real de POIs de um destino, listagem/preview e adoção (clonagem
 * datada) como um Itinerary real do usuário autenticado.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ItineraryTemplateService {

    private final ChatClient chatClient;
    private final DestinationRepository destinationRepo;
    private final PoiRepository poiRepo;
    private final ItineraryTemplateRepository templateRepo;
    private final ItineraryTemplateDayRepository templateDayRepo;
    private final ItineraryTemplateActivityRepository templateActivityRepo;
    private final ItineraryRepository itineraryRepo;
    private final ItineraryDayRepository itineraryDayRepo;
    private final ItineraryActivityRepository itineraryActivityRepo;
    private final UserRepository userRepo;

    public ItineraryTemplateDTO generateForDestination(String destinationSlug, int durationDays, BudgetTier budgetTier) {
        Destination destination = destinationRepo.findBySlug(destinationSlug)
            .orElseThrow(() -> new ResourceNotFoundException("Destino não encontrado"));

        List<Poi> pois = poiRepo.findAllByDestinationIdAndActiveTrue(destination.getId());
        Map<UUID, Poi> poiById = pois.stream().collect(Collectors.toMap(Poi::getId, p -> p));

        String prompt = buildGenerationPrompt(destination, durationDays, budgetTier, pois);

        GeneratedTemplateContent content = chatClient.prompt()
            .user(prompt)
            .call()
            .entity(GeneratedTemplateContent.class);

        List<ValidatedActivity> validActivities = (content.activities() == null ? List.<GeneratedTemplateActivity>of() : content.activities())
            .stream()
            .map(ga -> validateGeneratedActivity(ga, poiById))
            .filter(Objects::nonNull)
            .toList();

        Map<Integer, List<ValidatedActivity>> byDay = new TreeMap<>();
        for (ValidatedActivity va : validActivities) {
            byDay.computeIfAbsent(va.dayNumber(), k -> new ArrayList<>()).add(va);
        }

        ItineraryTemplate template = ItineraryTemplate.builder()
            .destination(destination)
            .title(content.title())
            .description(content.description())
            .durationDays(durationDays)
            .budgetTier(budgetTier)
            .build();

        List<ItineraryTemplateDay> days = new ArrayList<>();
        for (Map.Entry<Integer, List<ValidatedActivity>> entry : byDay.entrySet()) {
            ItineraryTemplateDay day = ItineraryTemplateDay.builder()
                .itineraryTemplate(template)
                .dayNumber(entry.getKey())
                .build();

            List<ItineraryTemplateActivity> activities = entry.getValue().stream()
                .map(va -> ItineraryTemplateActivity.builder()
                    .itineraryTemplateDay(day)
                    .poi(va.poi())
                    .slot(va.slot())
                    .orderInSlot(va.orderInSlot())
                    .build())
                .toList();

            day.setActivities(new ArrayList<>(activities));
            days.add(day);
        }
        template.setDays(days);

        // O cascade ALL em ItineraryTemplate#days / ItineraryTemplateDay#activities
        // (ver entidades) persiste dias e atividades junto do template.
        templateRepo.save(template);

        return new ItineraryTemplateDTO(
            template.getId(),
            template.getTitle(),
            template.getDescription(),
            template.getDurationDays(),
            template.getBudgetTier(),
            destination.getId()
        );
    }

    public List<ItineraryTemplateSummaryDTO> findAllForDestination(String destinationSlug) {
        Destination destination = destinationRepo.findBySlug(destinationSlug)
            .orElseThrow(() -> new ResourceNotFoundException("Destino não encontrado"));

        return templateRepo.findAllByDestinationId(destination.getId()).stream()
            .map(t -> new ItineraryTemplateSummaryDTO(
                t.getId(), t.getTitle(), t.getDescription(), t.getDurationDays(), t.getBudgetTier()
            ))
            .toList();
    }

    public ItineraryTemplateDetailDTO findById(UUID templateId) {
        ItineraryTemplate template = templateRepo.findById(templateId)
            .orElseThrow(() -> new ResourceNotFoundException("Template de roteiro não encontrado"));

        List<TemplateDayDTO> days = template.getDays().stream()
            .sorted(Comparator.comparing(ItineraryTemplateDay::getDayNumber))
            .map(day -> new TemplateDayDTO(
                day.getDayNumber(),
                day.getActivities().stream()
                    .sorted(Comparator.comparing(ItineraryTemplateActivity::getSlot)
                        .thenComparing(ItineraryTemplateActivity::getOrderInSlot))
                    .map(a -> new TemplateActivityDTO(a.getSlot().name(), a.getOrderInSlot(), PoiDetailDTO.from(a.getPoi())))
                    .toList()
            ))
            .toList();

        return new ItineraryTemplateDetailDTO(
            template.getId(),
            template.getTitle(),
            template.getDescription(),
            template.getDurationDays(),
            template.getBudgetTier(),
            template.getDestination().getId(),
            template.getDestination().getName(),
            days
        );
    }

    public ItineraryResponseDTO adopt(UUID templateId, UUID userId, LocalDate startDate) {
        ItineraryTemplate template = templateRepo.findById(templateId)
            .orElseThrow(() -> new ResourceNotFoundException("Template de roteiro não encontrado"));

        User user = userRepo.getReferenceById(userId);
        Destination destination = template.getDestination();
        LocalDate endDate = startDate.plusDays(template.getDurationDays() - 1);

        Itinerary itinerary = Itinerary.builder()
            .user(user)
            .destination(destination)
            .startDate(startDate)
            .endDate(endDate)
            .title(template.getTitle())
            .build();

        List<ItineraryDay> days = template.getDays().stream()
            .sorted(Comparator.comparing(ItineraryTemplateDay::getDayNumber))
            .map(templateDay -> {
                ItineraryDay day = ItineraryDay.builder()
                    .itinerary(itinerary)
                    .dayNumber(templateDay.getDayNumber())
                    .date(startDate.plusDays(templateDay.getDayNumber() - 1))
                    .build();

                List<ItineraryActivity> activities = templateDay.getActivities().stream()
                    .sorted(Comparator.comparing(ItineraryTemplateActivity::getSlot)
                        .thenComparing(ItineraryTemplateActivity::getOrderInSlot))
                    .map(templateActivity -> ItineraryActivity.builder()
                        .itineraryDay(day)
                        .poi(templateActivity.getPoi())
                        .slot(templateActivity.getSlot())
                        .orderInSlot(templateActivity.getOrderInSlot())
                        .build())
                    .toList();

                day.setActivities(new ArrayList<>(activities));
                return day;
            })
            .toList();

        itinerary.setDays(new ArrayList<>(days));

        // Cascade ALL em Itinerary#days / ItineraryDay#activities (mesmo
        // padrão usado por ItineraryEngineService ao persistir um roteiro
        // recém-gerado) persiste dias e atividades junto do itinerário.
        itineraryRepo.save(itinerary);

        List<DayDTO> dayDTOs = itinerary.getDays().stream()
            .map(day -> new DayDTO(
                day.getDayNumber(),
                day.getDate(),
                day.getActivities().stream()
                    .map(a -> new ActivityDTO(a.getId(), a.getSlot(), a.getOrderInSlot(), PoiDetailDTO.from(a.getPoi())))
                    .toList()
            ))
            .toList();

        return new ItineraryResponseDTO(
            itinerary.getId(),
            itinerary.getTitle(),
            destination.getId(),
            destination.getName(),
            destination.getHeroImageUrl(),
            itinerary.getStartDate(),
            itinerary.getEndDate(),
            dayDTOs
        );
    }

    private String buildGenerationPrompt(Destination destination, int durationDays, BudgetTier budgetTier, List<Poi> pois) {
        String poiCatalog = pois.stream()
            .map(p -> "- id: %s | title: %s | category: %s | budgetTier: %s | mustSee: %s"
                .formatted(p.getId(), p.getTitle(), p.getCategory(), p.getBudgetTier(), p.isMustSee()))
            .collect(Collectors.joining("\n"));

        return """
            Você é um especialista em roteiros de viagem. Monte um roteiro de %d dia(s) para o destino
            "%s", adequado ao orçamento %s, usando SOMENTE os pontos de interesse (POIs) listados abaixo —
            não invente nem utilize POIs fora desta lista.

            POIs disponíveis:
            %s

            Priorize os POIs com mustSee: true — eles são os pontos imperdíveis do destino e devem ser
            alocados primeiro, cada um no dia e período (MORNING, AFTERNOON ou EVENING) mais lógico dentro
            do roteiro, antes de preencher os horários restantes com os demais POIs. Monte a sequência de
            cada dia como um roteiro real e conhecido daquela cidade montaria — respeitando proximidade e
            fluxo geográfico, agrupando por categoria quando fizer sentido (ex: museus próximos na mesma
            janela, vida noturna à noite) — e não como uma escolha aleatória de POIs. Baseie-se em guias de viagem reais e publicados
            sobre %s para definir uma sequência de visitas coerente com o que um roteiro turístico de
            verdade sobre esse lugar teria.

            Cada dia deve ter atividades distribuídas nos períodos MORNING, AFTERNOON e EVENING.
            Responda SOMENTE com JSON válido, sem markdown, sem explicações.

            Formato exato:
            {
              "title": "Título do roteiro",
              "description": "Descrição breve do roteiro em 1-2 frases",
              "activities": [
                {
                  "poiId": "uuid do POI (deve ser um dos ids listados acima)",
                  "dayNumber": 1,
                  "slot": "MORNING",
                  "orderInSlot": 0
                }
              ]
            }
            """.formatted(durationDays, destination.getName(), budgetTier, poiCatalog, destination.getName());
    }

    private ValidatedActivity validateGeneratedActivity(GeneratedTemplateActivity ga, Map<UUID, Poi> poiById) {
        try {
            UUID poiId = UUID.fromString(ga.poiId());
            Poi poi = poiById.get(poiId);
            if (poi == null) {
                log.warn("poiId alucinado pelo LLM ignorado (não pertence ao catálogo do destino): {}", ga.poiId());
                return null;
            }

            ItinerarySlot slot = ItinerarySlot.valueOf(ga.slot().toUpperCase());

            if (ga.dayNumber() == null || ga.orderInSlot() == null) {
                log.warn("Atividade gerada ignorada (dayNumber/orderInSlot ausente): {}", ga);
                return null;
            }

            return new ValidatedActivity(poi, ga.dayNumber(), slot, ga.orderInSlot());
        } catch (Exception e) {
            log.warn("Atividade gerada ignorada (dados inválidos): {} — {}", ga, e.getMessage());
            return null;
        }
    }

    private record ValidatedActivity(Poi poi, Integer dayNumber, ItinerarySlot slot, Integer orderInSlot) {}
}
