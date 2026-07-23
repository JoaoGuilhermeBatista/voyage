package luvoygroup.voyage.features.itinerary;

import luvoygroup.voyage.domain.*;
import luvoygroup.voyage.domain.enums.*;
import luvoygroup.voyage.exception.*;
import luvoygroup.voyage.features.itinerary.dto.*;
import luvoygroup.voyage.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItineraryEngineService {

    private final DestinationRepository destinationRepo;
    private final OnboardingPreferencesRepository prefsRepo;
    private final PoiRepository poiRepo;
    private final ItineraryRepository itineraryRepo;
    private final ItineraryDayRepository itineraryDayRepo;
    private final ItineraryActivityRepository itineraryActivityRepo;
    private final UserRepository userRepo;

    private record SlotSpec(ItinerarySlot slot, int count) {}

    @Transactional
    public ItineraryResponseDTO generate(UUID userId, ItineraryRequest request) {
        // Step 1: Validate dates
        if (request.startDate().isBefore(LocalDate.now()))
            throw new InvalidDateRangeException("A data de início não pode ser no passado");
        if (request.startDate().isAfter(request.endDate()))
            throw new InvalidDateRangeException("A data de início deve ser anterior à data de fim");

        // Step 2: Load context
        OnboardingPreferences prefs = prefsRepo.findByUserId(userId)
            .orElseThrow(() -> new OnboardingNotCompletedException("Complete o onboarding antes de gerar um roteiro"));
        Destination destination = destinationRepo.findById(request.destinationId())
            .orElseThrow(() -> new ResourceNotFoundException("Destino não encontrado"));
        int dayCount = (int) ChronoUnit.DAYS.between(request.startDate(), request.endDate()) + 1;

        // Step 3: Filter POIs by budget
        List<Poi> candidates = poiRepo.findActiveByDestinationAndBudget(
            request.destinationId(), budgetTiers(prefs.getBudget()));
        if (candidates.size() < 5)
            throw new InsufficientContentException(
                "O destino ainda não tem POIs suficientes para gerar um roteiro. Mínimo: 5.");

        // Steps 4 & 5: Score and sort
        List<Poi> ranked = candidates.stream()
            .sorted(Comparator.comparingInt((Poi p) -> scoreFor(p, prefs)).reversed())
            .collect(Collectors.toCollection(ArrayList::new));

        log.info("Engine: {} POIs candidatos para {} dias em {}", ranked.size(), dayCount, destination.getName());

        // Step 6, 7, 8: Distribute, order by proximity, persist
        String title = destination.getName() + " — " + dayCount + (dayCount == 1 ? " dia" : " dias");
        User userRef = userRepo.getReferenceById(userId);

        Itinerary itinerary = itineraryRepo.save(Itinerary.builder()
            .user(userRef)
            .destination(destination)
            .startDate(request.startDate())
            .endDate(request.endDate())
            .title(title)
            .build());

        List<SlotSpec> slotSpecs = slotSpecs(prefs.getRhythm());
        Set<UUID> usedIds = new HashSet<>();
        List<DayDTO> dayDTOs = new ArrayList<>();

        for (int d = 0; d < dayCount; d++) {
            ItineraryDay day = itineraryDayRepo.save(ItineraryDay.builder()
                .itinerary(itinerary)
                .dayNumber(d + 1)
                .date(request.startDate().plusDays(d))
                .build());

            List<ActivityDTO> activityDTOs = new ArrayList<>();

            for (SlotSpec spec : slotSpecs) {
                List<Poi> slotPois = pickPois(ranked, usedIds, spec.slot(), spec.count());
                List<Poi> ordered = nearestNeighbor(slotPois);

                for (int i = 0; i < ordered.size(); i++) {
                    Poi poi = ordered.get(i);
                    ItineraryActivity saved = itineraryActivityRepo.save(ItineraryActivity.builder()
                        .itineraryDay(day)
                        .poi(poi)
                        .slot(spec.slot())
                        .orderInSlot(i)
                        .build());
                    activityDTOs.add(new ActivityDTO(saved.getId(), spec.slot(), i, PoiDetailDTO.from(poi)));
                }
            }

            dayDTOs.add(new DayDTO(d + 1, request.startDate().plusDays(d), activityDTOs));
        }

        log.info("Engine: roteiro '{}' gerado com {} dias", title, dayCount);

        return new ItineraryResponseDTO(
            itinerary.getId(), title,
            destination.getId(), destination.getName(), destination.getHeroImageUrl(),
            request.startDate(), request.endDate(),
            dayDTOs
        );
    }

    private List<Poi> pickPois(List<Poi> ranked, Set<UUID> usedIds, ItinerarySlot slot, int count) {
        List<Poi> result = new ArrayList<>();
        for (Poi poi : ranked) {
            if (result.size() >= count) break;
            if (usedIds.contains(poi.getId())) continue;
            if (!isCompatible(poi, slot)) continue;
            result.add(poi);
            usedIds.add(poi.getId());
        }
        return result;
    }

    private boolean isCompatible(Poi poi, ItinerarySlot slot) {
        if (poi.getOpeningTime() == null) return true;
        return switch (slot) {
            case MORNING -> !poi.getOpeningTime().isAfter(java.time.LocalTime.of(9, 0));
            case AFTERNOON -> !poi.getOpeningTime().isAfter(java.time.LocalTime.of(13, 0));
            case EVENING -> !poi.getOpeningTime().isAfter(java.time.LocalTime.of(18, 0));
        };
    }

    private int scoreFor(Poi poi, OnboardingPreferences prefs) {
        int score = 0;
        for (String interest : prefs.getInterests()) {
            if (poi.getTags().contains(interest)) score += 10;
        }
        score += groupBonus(poi.getCategory(), prefs.getGroupType());
        return score;
    }

    private int groupBonus(PoiCategory cat, GroupType groupType) {
        return switch (groupType) {
            case COUPLE -> switch (cat) {
                case RESTAURANT -> 5;
                case BEACH -> 3;
                case HOTEL -> 3;
                default -> 0;
            };
            case FAMILY -> switch (cat) {
                case PARK -> 5;
                case BEACH -> 3;
                case SHOPPING -> 2;
                default -> 0;
            };
            case SOLO -> switch (cat) {
                case MUSEUM -> 5;
                case NIGHTLIFE -> 3;
                default -> 0;
            };
            case GROUP -> switch (cat) {
                case SHOPPING -> 3;
                case NIGHTLIFE -> 3;
                case PARK -> 2;
                default -> 0;
            };
        };
    }

    private List<Poi> nearestNeighbor(List<Poi> pois) {
        if (pois.size() <= 1) return pois;
        boolean hasCoords = pois.stream()
            .allMatch(p -> p.getLatitude() != null && p.getLongitude() != null);
        if (!hasCoords) return pois;

        List<Poi> result = new ArrayList<>();
        List<Poi> remaining = new ArrayList<>(pois);
        result.add(remaining.remove(0));

        while (!remaining.isEmpty()) {
            Poi last = result.getLast();
            Poi nearest = remaining.stream()
                .min(Comparator.comparingDouble(p ->
                    haversineKm(last.getLatitude(), last.getLongitude(),
                        p.getLatitude(), p.getLongitude())))
                .orElseThrow();
            result.add(nearest);
            remaining.remove(nearest);
        }
        return result;
    }

    private static double haversineKm(BigDecimal lat1, BigDecimal lon1,
                                       BigDecimal lat2, BigDecimal lon2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2.doubleValue() - lat1.doubleValue());
        double dLon = Math.toRadians(lon2.doubleValue() - lon1.doubleValue());
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(lat1.doubleValue()))
            * Math.cos(Math.toRadians(lat2.doubleValue()))
            * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private List<BudgetTier> budgetTiers(BudgetTier budget) {
        return switch (budget) {
            case ECONOMY -> List.of(BudgetTier.ECONOMY);
            case MODERATE -> List.of(BudgetTier.ECONOMY, BudgetTier.MODERATE);
            case LUXURY -> List.of(BudgetTier.ECONOMY, BudgetTier.MODERATE, BudgetTier.LUXURY);
        };
    }

    private List<SlotSpec> slotSpecs(TravelRhythm rhythm) {
        return switch (rhythm) {
            case RELAXED -> List.of(
                new SlotSpec(ItinerarySlot.MORNING, 1),
                new SlotSpec(ItinerarySlot.AFTERNOON, 1)
            );
            case MODERATE -> List.of(
                new SlotSpec(ItinerarySlot.MORNING, 1),
                new SlotSpec(ItinerarySlot.AFTERNOON, 1),
                new SlotSpec(ItinerarySlot.EVENING, 1)
            );
            case INTENSE -> List.of(
                new SlotSpec(ItinerarySlot.MORNING, 1),
                new SlotSpec(ItinerarySlot.AFTERNOON, 2),
                new SlotSpec(ItinerarySlot.EVENING, 1)
            );
        };
    }
}
