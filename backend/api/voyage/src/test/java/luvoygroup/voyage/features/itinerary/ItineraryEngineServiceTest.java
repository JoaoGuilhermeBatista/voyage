package luvoygroup.voyage.features.itinerary;

import luvoygroup.voyage.domain.*;
import luvoygroup.voyage.domain.enums.*;
import luvoygroup.voyage.exception.*;
import luvoygroup.voyage.features.itinerary.dto.ItineraryRequest;
import luvoygroup.voyage.features.itinerary.dto.ItineraryResponseDTO;
import luvoygroup.voyage.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ItineraryEngineServiceTest {

    @Mock DestinationRepository destinationRepo;
    @Mock OnboardingPreferencesRepository prefsRepo;
    @Mock PoiRepository poiRepo;
    @Mock ItineraryRepository itineraryRepo;
    @Mock ItineraryDayRepository itineraryDayRepo;
    @Mock ItineraryActivityRepository itineraryActivityRepo;
    @Mock UserRepository userRepo;

    @InjectMocks ItineraryEngineService engineService;

    final UUID userId = UUID.randomUUID();
    final UUID destinationId = UUID.randomUUID();
    Destination destination;
    OnboardingPreferences prefs;
    User user;

    @BeforeEach
    void setup() {
        destination = Destination.builder()
            .id(destinationId).name("Paris").city("Paris").country("França")
            .slug("paris").tagline("Cidade Luz").active(true).build();

        user = User.builder().id(userId).email("test@luvoy.com").build();

        prefs = OnboardingPreferences.builder()
            .id(UUID.randomUUID()).user(user)
            .budget(BudgetTier.MODERATE).rhythm(TravelRhythm.MODERATE)
            .groupType(GroupType.COUPLE).interests(List.of("cultura", "gastronomia"))
            .build();

        // Default mocks for repos that save entities
        when(itineraryRepo.save(any())).thenAnswer(inv -> {
            Itinerary i = inv.getArgument(0);
            i.setId(UUID.randomUUID());
            return i;
        });
        when(itineraryDayRepo.save(any())).thenAnswer(inv -> {
            ItineraryDay d = inv.getArgument(0);
            d.setId(UUID.randomUUID());
            return d;
        });
        when(itineraryActivityRepo.save(any())).thenAnswer(inv -> {
            ItineraryActivity a = inv.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });
        when(userRepo.getReferenceById(userId)).thenReturn(user);
    }

    // ─── Teste 1: Onboarding não concluído ───────────────────────────────────

    @Test
    void teste1_shouldThrow_whenUserHasNotCompletedOnboarding() {
        when(prefsRepo.findByUserId(userId)).thenReturn(Optional.empty());

        ItineraryRequest request = new ItineraryRequest(
            destinationId, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));

        assertThrows(OnboardingNotCompletedException.class,
            () -> engineService.generate(userId, request));
    }

    // ─── Teste 2: POIs insuficientes ─────────────────────────────────────────

    @Test
    void teste2_shouldThrow_whenDestinationHasTooFewPois() {
        when(prefsRepo.findByUserId(userId)).thenReturn(Optional.of(prefs));
        when(destinationRepo.findById(destinationId)).thenReturn(Optional.of(destination));
        when(poiRepo.findActiveByDestinationAndBudget(eq(destinationId), anyList()))
            .thenReturn(List.of(makePoi(BudgetTier.MODERATE), makePoi(BudgetTier.MODERATE)));

        ItineraryRequest request = new ItineraryRequest(
            destinationId, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));

        assertThrows(InsufficientContentException.class,
            () -> engineService.generate(userId, request));
    }

    // ─── Teste 3: Data de início depois da data de fim ────────────────────────

    @Test
    void teste3_shouldThrow_whenStartDateIsAfterEndDate() {
        ItineraryRequest request = new ItineraryRequest(
            destinationId,
            LocalDate.of(2027, 8, 10),
            LocalDate.of(2027, 8, 5));

        assertThrows(InvalidDateRangeException.class,
            () -> engineService.generate(userId, request));
    }

    // ─── Teste 4: Data de início no passado ──────────────────────────────────

    @Test
    void teste4_shouldThrow_whenStartDateIsInThePast() {
        ItineraryRequest request = new ItineraryRequest(
            destinationId,
            LocalDate.now().minusDays(1),
            LocalDate.now().plusDays(3));

        assertThrows(InvalidDateRangeException.class,
            () -> engineService.generate(userId, request));
    }

    // ─── Teste 5: POIs não se repetem entre dias ─────────────────────────────

    @Test
    void teste5_shouldNotRepeatPoisAcrossDays() {
        when(prefsRepo.findByUserId(userId)).thenReturn(Optional.of(prefs));
        when(destinationRepo.findById(destinationId)).thenReturn(Optional.of(destination));
        when(poiRepo.findActiveByDestinationAndBudget(eq(destinationId), anyList()))
            .thenReturn(makePoiList(15, BudgetTier.MODERATE));

        ItineraryRequest request = new ItineraryRequest(
            destinationId, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));

        ItineraryResponseDTO result = engineService.generate(userId, request);

        List<UUID> allPoiIds = result.days().stream()
            .flatMap(d -> d.activities().stream())
            .map(a -> a.poi().id())
            .toList();

        assertThat(allPoiIds).doesNotHaveDuplicates();
    }

    // ─── Teste 6: Filtro de budget ────────────────────────────────────────────

    @Test
    void teste6_shouldOnlyIncludeEconomyPois_whenUserBudgetIsEconomy() {
        OnboardingPreferences economyPrefs = OnboardingPreferences.builder()
            .id(UUID.randomUUID()).user(user)
            .budget(BudgetTier.ECONOMY).rhythm(TravelRhythm.MODERATE)
            .groupType(GroupType.SOLO).interests(List.of())
            .build();

        when(prefsRepo.findByUserId(userId)).thenReturn(Optional.of(economyPrefs));
        when(destinationRepo.findById(destinationId)).thenReturn(Optional.of(destination));

        List<Poi> economyOnly = makePoiList(10, BudgetTier.ECONOMY);
        when(poiRepo.findActiveByDestinationAndBudget(eq(destinationId), anyList()))
            .thenReturn(economyOnly);

        ItineraryRequest request = new ItineraryRequest(
            destinationId, LocalDate.now().plusDays(1), LocalDate.now().plusDays(1));

        ItineraryResponseDTO result = engineService.generate(userId, request);

        result.days().forEach(day ->
            day.activities().forEach(act ->
                assertEquals(BudgetTier.ECONOMY, act.poi().budgetTier())));
    }

    // ─── Regra de negócio 1: Atividades por ritmo RELAXED ────────────────────

    @Test
    void regraRhythm_shouldGenerate2ActivitiesPerDay_whenRelaxed() {
        OnboardingPreferences relaxed = prefsWithRhythm(TravelRhythm.RELAXED);
        when(prefsRepo.findByUserId(userId)).thenReturn(Optional.of(relaxed));
        when(destinationRepo.findById(destinationId)).thenReturn(Optional.of(destination));
        when(poiRepo.findActiveByDestinationAndBudget(eq(destinationId), anyList()))
            .thenReturn(makePoiList(15, BudgetTier.MODERATE));

        ItineraryRequest request = new ItineraryRequest(
            destinationId, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));

        ItineraryResponseDTO result = engineService.generate(userId, request);

        result.days().forEach(day ->
            assertEquals(2, day.activities().size(),
                "RELAXED deve ter 2 atividades por dia"));
    }

    // ─── Regra de negócio 1: Atividades por ritmo MODERATE ───────────────────

    @Test
    void regraRhythm_shouldGenerate3ActivitiesPerDay_whenModerate() {
        when(prefsRepo.findByUserId(userId)).thenReturn(Optional.of(prefs));
        when(destinationRepo.findById(destinationId)).thenReturn(Optional.of(destination));
        when(poiRepo.findActiveByDestinationAndBudget(eq(destinationId), anyList()))
            .thenReturn(makePoiList(15, BudgetTier.MODERATE));

        ItineraryRequest request = new ItineraryRequest(
            destinationId, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));

        ItineraryResponseDTO result = engineService.generate(userId, request);

        result.days().forEach(day ->
            assertEquals(3, day.activities().size(),
                "MODERATE deve ter 3 atividades por dia"));
    }

    // ─── Regra de negócio 1: Atividades por ritmo INTENSE ────────────────────

    @Test
    void regraRhythm_shouldGenerate4ActivitiesPerDay_whenIntense() {
        OnboardingPreferences intense = prefsWithRhythm(TravelRhythm.INTENSE);
        when(prefsRepo.findByUserId(userId)).thenReturn(Optional.of(intense));
        when(destinationRepo.findById(destinationId)).thenReturn(Optional.of(destination));
        when(poiRepo.findActiveByDestinationAndBudget(eq(destinationId), anyList()))
            .thenReturn(makePoiList(20, BudgetTier.MODERATE));

        ItineraryRequest request = new ItineraryRequest(
            destinationId, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));

        ItineraryResponseDTO result = engineService.generate(userId, request);

        result.days().forEach(day ->
            assertEquals(4, day.activities().size(),
                "INTENSE deve ter 4 atividades por dia"));
    }

    // ─── Regra de negócio 8: Roteiro de 1 dia funciona corretamente ──────────

    @Test
    void regraUmDia_shouldGenerateOneDayItinerary_whenStartEqualsEnd() {
        when(prefsRepo.findByUserId(userId)).thenReturn(Optional.of(prefs));
        when(destinationRepo.findById(destinationId)).thenReturn(Optional.of(destination));
        when(poiRepo.findActiveByDestinationAndBudget(eq(destinationId), anyList()))
            .thenReturn(makePoiList(10, BudgetTier.MODERATE));

        LocalDate today = LocalDate.now().plusDays(1);
        ItineraryRequest request = new ItineraryRequest(destinationId, today, today);

        ItineraryResponseDTO result = engineService.generate(userId, request);

        assertEquals(1, result.days().size());
        assertEquals(1, result.days().get(0).dayNumber());
    }

    // ─── Regra de negócio 7: Slots vazios quando POIs insuficientes ──────────

    @Test
    void regraPoiInsuficiente_shouldNotThrow_whenFewerPoisThanSlots() {
        OnboardingPreferences intense = prefsWithRhythm(TravelRhythm.INTENSE);
        when(prefsRepo.findByUserId(userId)).thenReturn(Optional.of(intense));
        when(destinationRepo.findById(destinationId)).thenReturn(Optional.of(destination));
        // Apenas 6 POIs para 3 dias com ritmo INTENSE (precisaria de 12)
        when(poiRepo.findActiveByDestinationAndBudget(eq(destinationId), anyList()))
            .thenReturn(makePoiList(6, BudgetTier.MODERATE));

        ItineraryRequest request = new ItineraryRequest(
            destinationId, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));

        assertDoesNotThrow(() -> engineService.generate(userId, request));

        ItineraryResponseDTO result = engineService.generate(userId, request);
        result.days().forEach(day ->
            assertTrue(day.activities().size() <= 4,
                "Nenhum dia deve ter mais de 4 atividades"));
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private Poi makePoi(BudgetTier tier) {
        return Poi.builder()
            .id(UUID.randomUUID())
            .destination(destination)
            .title("POI-" + UUID.randomUUID().toString().substring(0, 6))
            .category(PoiCategory.MUSEUM)
            .budgetTier(tier)
            .tags(List.of("cultura"))
            .active(true)
            .latitude(BigDecimal.valueOf(48.8 + Math.random() * 0.1))
            .longitude(BigDecimal.valueOf(2.3 + Math.random() * 0.1))
            .averageDurationMinutes(90)
            .build();
    }

    private List<Poi> makePoiList(int count, BudgetTier tier) {
        List<Poi> list = new ArrayList<>();
        for (int i = 0; i < count; i++) list.add(makePoi(tier));
        return list;
    }

    private OnboardingPreferences prefsWithRhythm(TravelRhythm rhythm) {
        return OnboardingPreferences.builder()
            .id(UUID.randomUUID()).user(user)
            .budget(BudgetTier.MODERATE).rhythm(rhythm)
            .groupType(GroupType.COUPLE).interests(List.of("cultura"))
            .build();
    }
}
