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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
        // Fallback para quando a LLM devolve o TÍTULO do POI em vez do UUID
        // pedido no prompt (achado em validação manual ao vivo) — ver
        // resolvePoi(). (a, b) -> a: em caso de títulos duplicados no mesmo
        // destino, fica com o primeiro; não é um cenário esperado hoje.
        Map<String, Poi> poiByNormalizedTitle = pois.stream()
            .collect(Collectors.toMap(p -> normalizeTitle(p.getTitle()), p -> p, (a, b) -> a));

        // Agrupamento geográfico feito em CÓDIGO (não só por instrução de
        // texto pro LLM) — ver clusterPoisByDay(). Achado real ao comparar
        // contra um roteiro de referência da contratante: mesmo com
        // instrução explícita de proximidade no prompt, a LLM ocasionalmente
        // escolhia um POI a ~10km de distância dos outros do mesmo dia
        // (geralmente por bater a categoria certa, ex: "SHOPPING", ignorando
        // a distância). O dayNumber devolvido pela LLM é sobrescrito pelo
        // cluster real em validateGeneratedActivity — geograficamente
        // garantido, não apenas sugerido.
        Map<UUID, Integer> dayCluster = clusterPoisByDay(pois, durationDays);

        String prompt = buildGenerationPrompt(destination, durationDays, budgetTier, pois, dayCluster);

        GeneratedTemplateContent content = chatClient.prompt()
            .user(prompt)
            .call()
            .entity(GeneratedTemplateContent.class);

        List<ValidatedActivity> validActivities = (content.activities() == null ? List.<GeneratedTemplateActivity>of() : content.activities())
            .stream()
            .map(ga -> validateGeneratedActivity(ga, poiById, poiByNormalizedTitle, dayCluster))
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
            destination.getCountry(),
            itinerary.getStartDate(),
            itinerary.getEndDate(),
            dayDTOs
        );
    }

    private String buildGenerationPrompt(
        Destination destination, int durationDays, BudgetTier budgetTier, List<Poi> pois, Map<UUID, Integer> dayCluster
    ) {
        // Catálogo dividido por dia, seguindo o cluster geográfico já
        // calculado em código (clusterPoisByDay) — cada seção só lista os
        // POIs daquela região, então a LLM não tem nem a OPÇÃO de escolher
        // um POI distante pro dia errado (antes disso, a instrução de
        // proximidade era só textual e a LLM ignorava ocasionalmente).
        StringBuilder catalogBuilder = new StringBuilder();
        for (int day = 1; day <= durationDays; day++) {
            int currentDay = day;
            List<Poi> dayPois = pois.stream()
                .filter(p -> dayCluster.getOrDefault(p.getId(), 1) == currentDay)
                .toList();
            catalogBuilder.append("Dia %d — use POIs SOMENTE desta lista para este dia (já filtrados por proximidade geográfica):\n"
                .formatted(currentDay));
            for (Poi p : dayPois) {
                catalogBuilder.append("- id: %s | title: %s | category: %s | budgetTier: %s | address: %s | mustSee: %s\n"
                    .formatted(p.getId(), p.getTitle(), p.getCategory(), p.getBudgetTier(),
                        p.getAddress() == null ? "desconhecido" : p.getAddress(), p.isMustSee()));
            }
            catalogBuilder.append("\n");
        }
        String poiCatalog = catalogBuilder.toString();

        return """
            Você é um especialista em roteiros de viagem. Monte um roteiro de %d dia(s) para o destino
            "%s", adequado ao orçamento %s, usando SOMENTE os pontos de interesse (POIs) listados abaixo —
            não invente nem utilize POIs fora desta lista.

            O catálogo já vem DIVIDIDO POR DIA (uma seção "Dia N" para cada dia do roteiro) — essa divisão
            já foi calculada por proximidade geográfica real (coordenadas), então os POIs de cada seção já
            estão concentrados na mesma região da cidade. REGRA OBRIGATÓRIA: use exclusivamente os POIs da
            seção "Dia N" para montar as atividades do dia N — nunca pegue um POI de uma seção pra colocar
            em outro dia. Isso é o que garante que o roteiro de cada dia fique geograficamente coerente (a
            pé ou com deslocamento curto), sem pular entre pontos distantes da cidade no mesmo dia.

            %s

            Priorize os POIs com mustSee: true — eles são os pontos imperdíveis do destino e devem ser
            alocados primeiro, cada um no período (MORNING, AFTERNOON ou EVENING) mais lógico dentro do
            dia em que já estão (dado pela seção), antes de preencher os horários restantes com os demais
            POIs da mesma seção. Dentro de cada dia, monte a sequência como um roteiro real e conhecido
            daquela cidade montaria, agrupando por categoria quando fizer sentido (ex: museus próximos na
            mesma janela, vida noturna à noite) — e não como uma escolha aleatória de POIs. Baseie-se em
            guias de viagem reais e publicados sobre %s para definir uma sequência de visitas coerente com
            o que um roteiro turístico de verdade sobre esse lugar teria.

            DENSIDADE: um roteiro real tem vários pontos por período, não só um. Cada período (MORNING e
            AFTERNOON, principalmente) deve ter, tipicamente, de 2 a 4 POIs em sequência — a menos que um
            único POI já ocupe o período inteiro por natureza (ex: um museu muito grande) ou que a seção
            daquele dia seja pequena demais para sustentar essa densidade.

            REFEIÇÕES: sempre que existir POI de categoria RESTAURANT na seção do dia, compatível com o
            orçamento, garanta pelo menos uma refeição por dia — o almoço como primeiro item (orderInSlot 0)
            do período AFTERNOON, e o jantar dentro do período EVENING.

            VARIEDADE: não concentre todos os POIs mustSee/imperdíveis nos primeiros dias do roteiro —
            distribua-os ao longo de toda a viagem. Se um dia já reuniu vários POIs mustSee, prefira que
            outro dia (não precisa ser o seguinte) tenha mais POIs comuns/não-mustSee da seção — dá a
            sensação de descoberta local em vez de um roteiro só de pontos turísticos óbvios em sequência.

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

    private ValidatedActivity validateGeneratedActivity(
        GeneratedTemplateActivity ga, Map<UUID, Poi> poiById, Map<String, Poi> poiByNormalizedTitle, Map<UUID, Integer> dayCluster
    ) {
        try {
            Poi poi = resolvePoi(ga.poiId(), poiById, poiByNormalizedTitle);
            if (poi == null) {
                log.warn("poiId alucinado pelo LLM ignorado (não pertence ao catálogo do destino, nem por id nem por título): {}", ga.poiId());
                return null;
            }

            ItinerarySlot slot = ItinerarySlot.valueOf(ga.slot().toUpperCase());

            if (ga.orderInSlot() == null) {
                log.warn("Atividade gerada ignorada (orderInSlot ausente): {}", ga);
                return null;
            }

            // O dayNumber é SOBRESCRITO pelo cluster geográfico real (nunca
            // confia no que a LLM devolveu para esse campo) — garante que o
            // agrupamento por dia reflita a divisão geográfica calculada em
            // clusterPoisByDay, mesmo se a LLM tiver misturado dias. Só cai
            // no valor devolvido pela LLM se o POI não tiver cluster (não
            // deveria acontecer hoje — todo POI ativo tem lat/lng).
            Integer dayNumber = dayCluster.getOrDefault(poi.getId(), ga.dayNumber());
            if (dayNumber == null) {
                log.warn("Atividade gerada ignorada (dayNumber ausente e sem cluster geográfico): {}", ga);
                return null;
            }

            return new ValidatedActivity(poi, dayNumber, slot, ga.orderInSlot());
        } catch (Exception e) {
            log.warn("Atividade gerada ignorada (dados inválidos): {} — {}", ga, e.getMessage());
            return null;
        }
    }

    /**
     * Agrupa os POIs em até {@code dayCount} clusters geográficos —
     * k-means simples sobre (latitude, longitude), com a longitude
     * corrigida por cos(latitude média) pra não distorcer a distância
     * leste-oeste. Retorna poiId -> número do dia (1-based).
     *
     * Determinístico de propósito (sementes iniciais escolhidas por
     * ordenação, não aleatórias) — o mesmo catálogo sempre produz o mesmo
     * agrupamento, facilita testar e comparar execuções.
     *
     * Por que em código e não só via prompt: comparação manual contra um
     * roteiro de referência real (contratante, Paris) mostrou a LLM
     * escolhendo POIs a ~10km de distância dos outros do mesmo dia mesmo
     * com instrução explícita de proximidade no texto — geralmente por
     * casar a categoria certa (ex: "SHOPPING") ignorando a distância. O
     * cluster geográfico feito aqui é estrutural: o catálogo do prompt já
     * vem pré-filtrado por dia (ver buildGenerationPrompt), e o dayNumber
     * final é sempre sobrescrito por este cluster em
     * validateGeneratedActivity — não depende da LLM obedecer.
     *
     * dayCount <= 1 ou nenhum POI com coordenadas: sem clusterização
     * real, todo mundo cai no dia 1 (mesmo comportamento de antes desta
     * mudança).
     */
    private Map<UUID, Integer> clusterPoisByDay(List<Poi> pois, int dayCount) {
        Map<UUID, Integer> result = new HashMap<>();

        List<Poi> withCoords = pois.stream()
            .filter(p -> p.getLatitude() != null && p.getLongitude() != null)
            .toList();

        if (dayCount <= 1 || withCoords.isEmpty()) {
            for (Poi p : pois) {
                result.put(p.getId(), 1);
            }
            return result;
        }

        double avgLat = withCoords.stream().mapToDouble(p -> p.getLatitude().doubleValue()).average().orElse(0);
        double lngScale = Math.cos(Math.toRadians(avgLat));

        int k = Math.min(dayCount, withCoords.size());

        List<Poi> sortedByLat = withCoords.stream()
            .sorted(Comparator.comparingDouble(p -> p.getLatitude().doubleValue()))
            .toList();
        double[][] centroids = new double[k][2];
        for (int i = 0; i < k; i++) {
            // Divisão por piso (sem +0.5 de offset): garante índices
            // estritamente crescentes e distintos mesmo quando o catálogo
            // tem exatamente k POIs (o offset de arredondamento colapsava
            // duas sementes no mesmo índice nesse caso-limite).
            int idx = Math.min((int) ((long) i * sortedByLat.size() / k), sortedByLat.size() - 1);
            Poi seed = sortedByLat.get(idx);
            centroids[i][0] = seed.getLatitude().doubleValue();
            centroids[i][1] = seed.getLongitude().doubleValue() * lngScale;
        }

        int[] assignment = new int[withCoords.size()];
        for (int iter = 0; iter < 20; iter++) {
            boolean changed = false;
            for (int i = 0; i < withCoords.size(); i++) {
                Poi p = withCoords.get(i);
                double lat = p.getLatitude().doubleValue();
                double lng = p.getLongitude().doubleValue() * lngScale;
                int best = 0;
                double bestDist = Double.MAX_VALUE;
                for (int c = 0; c < k; c++) {
                    double dist = Math.hypot(lat - centroids[c][0], lng - centroids[c][1]);
                    if (dist < bestDist) {
                        bestDist = dist;
                        best = c;
                    }
                }
                if (assignment[i] != best) {
                    changed = true;
                    assignment[i] = best;
                }
            }

            double[][] sums = new double[k][2];
            int[] counts = new int[k];
            for (int i = 0; i < withCoords.size(); i++) {
                Poi p = withCoords.get(i);
                int c = assignment[i];
                sums[c][0] += p.getLatitude().doubleValue();
                sums[c][1] += p.getLongitude().doubleValue() * lngScale;
                counts[c]++;
            }
            for (int c = 0; c < k; c++) {
                if (counts[c] > 0) {
                    centroids[c][0] = sums[c][0] / counts[c];
                    centroids[c][1] = sums[c][1] / counts[c];
                }
            }
            if (!changed) break;
        }

        for (int i = 0; i < withCoords.size(); i++) {
            result.put(withCoords.get(i).getId(), assignment[i] + 1);
        }
        // POIs sem coordenadas ficam de fora do mapa — se a LLM ainda assim
        // escolher um (catálogo os inclui em nenhuma seção específica hoje,
        // já que todo POI ativo tem lat/lng — ver PlacesServiceTest/dados de
        // produção), o dayNumber dela é aceito como fallback (ver
        // validateGeneratedActivity).
        return result;
    }

    /**
     * Resolve o poiId cru devolvido pela LLM contra o catálogo real do
     * destino. Caminho normal: {@code poiIdRaw} é um UUID válido presente em
     * {@code poiById}. Fallback (achado em validação manual ao vivo — ver
     * teste {@code generateForDestination_shouldRecoverActivity_whenLlmReturnsPoiTitleInsteadOfUuid}):
     * a LLM às vezes devolve o TÍTULO do POI em vez do UUID pedido
     * explicitamente no prompt; nesse caso, antes de descartar a atividade
     * (o que deixaria um período vazio no roteiro), tenta casar
     * {@code poiIdRaw} contra os títulos reais do catálogo (normalizados:
     * trim + lower-case). Nunca inventa/aceita um POI fora do catálogo —
     * apenas recupera atividades que já apontavam para um POI real, só que
     * pelo campo errado.
     */
    private Poi resolvePoi(String poiIdRaw, Map<UUID, Poi> poiById, Map<String, Poi> poiByNormalizedTitle) {
        try {
            UUID poiId = UUID.fromString(poiIdRaw);
            Poi poi = poiById.get(poiId);
            if (poi != null) {
                return poi;
            }
        } catch (IllegalArgumentException notAUuid) {
            // poiIdRaw não é um UUID sintaticamente válido — segue para o
            // fallback por título abaixo em vez de propagar a exceção.
        }
        return poiByNormalizedTitle.get(normalizeTitle(poiIdRaw));
    }

    private static String normalizeTitle(String title) {
        return title == null ? "" : title.trim().toLowerCase(Locale.ROOT);
    }

    private record ValidatedActivity(Poi poi, Integer dayNumber, ItinerarySlot slot, Integer orderInSlot) {}
}
