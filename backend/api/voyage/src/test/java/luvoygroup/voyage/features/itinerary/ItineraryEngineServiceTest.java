package luvoygroup.voyage.features.itinerary;

import luvoygroup.voyage.domain.*;
import luvoygroup.voyage.domain.enums.*;
import luvoygroup.voyage.exception.*;
import luvoygroup.voyage.features.itinerary.dto.ItineraryRequest;
import luvoygroup.voyage.features.itinerary.dto.ItineraryResponseDTO;
import luvoygroup.voyage.features.maps.GeocodingService;
import luvoygroup.voyage.features.maps.dto.LatLng;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
    @Mock GeocodingService geocodingService;

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

        // Default: sem geocoding real configurado nos testes que não focam em
        // stayArea/proximidade — geocode() nunca resolve nada por padrão.
        when(geocodingService.geocode(anyString())).thenReturn(Optional.empty());
    }

    // ─── Teste 1: Onboarding não concluído ───────────────────────────────────

    @Test
    void teste1_shouldThrow_whenUserHasNotCompletedOnboarding() {
        when(prefsRepo.findByUserId(userId)).thenReturn(Optional.empty());

        ItineraryRequest request = new ItineraryRequest(
            destinationId, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3), null, null, null, null, null, null);

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
            destinationId, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3), null, null, null, null, null, null);

        assertThrows(InsufficientContentException.class,
            () -> engineService.generate(userId, request));
    }

    // ─── Teste 3: Data de início depois da data de fim ────────────────────────

    @Test
    void teste3_shouldThrow_whenStartDateIsAfterEndDate() {
        ItineraryRequest request = new ItineraryRequest(
            destinationId,
            LocalDate.of(2027, 8, 10),
            LocalDate.of(2027, 8, 5), null, null, null, null, null, null);

        assertThrows(InvalidDateRangeException.class,
            () -> engineService.generate(userId, request));
    }

    // ─── Teste 4: Data de início no passado ──────────────────────────────────

    @Test
    void teste4_shouldThrow_whenStartDateIsInThePast() {
        ItineraryRequest request = new ItineraryRequest(
            destinationId,
            LocalDate.now().minusDays(1),
            LocalDate.now().plusDays(3), null, null, null, null, null, null);

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
            destinationId, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3), null, null, null, null, null, null);

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
            destinationId, LocalDate.now().plusDays(1), LocalDate.now().plusDays(1), null, null, null, null, null, null);

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
            destinationId, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3), null, null, null, null, null, null);

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
            destinationId, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3), null, null, null, null, null, null);

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
            destinationId, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3), null, null, null, null, null, null);

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
        ItineraryRequest request = new ItineraryRequest(destinationId, today, today, null, null, null, null, null, null);

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
        // Apenas 9 POIs para 3 dias com ritmo INTENSE (precisaria de 12) — acima do
        // mínimo global de 8 (ver ItineraryEngineService.generate), mas ainda
        // insuficiente para cobrir todos os slots sem reuso.
        when(poiRepo.findActiveByDestinationAndBudget(eq(destinationId), anyList()))
            .thenReturn(makePoiList(9, BudgetTier.MODERATE));

        ItineraryRequest request = new ItineraryRequest(
            destinationId, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3), null, null, null, null, null, null);

        assertDoesNotThrow(() -> engineService.generate(userId, request));

        ItineraryResponseDTO result = engineService.generate(userId, request);
        result.days().forEach(day ->
            assertTrue(day.activities().size() <= 4,
                "Nenhum dia deve ter mais de 4 atividades"));
    }

    // ─── Regra de negócio nova: bônus de pontuação para POIs mustSee ─────────
    //
    // Contrato assumido (RED por design — a implementar por um próximo
    // agente em ItineraryEngineService#scoreFor): POIs marcados com
    // mustSee=true devem receber um bônus de pontuação que os coloque à
    // frente de concorrentes idênticos em tudo mais (mesma categoria, sem
    // tags que dariam bônus de interesse, mesmo grupo/vibe bonus). Hoje
    // scoreFor não considera mustSee, então com todos os candidatos
    // empatados em pontuação (score=0), a ordenação (stream.sorted, que é
    // estável) preserva a ordem de entrada da lista retornada pelo
    // repositório — por isso colocamos o POI mustSee=true DELIBERADAMENTE
    // no meio da lista de candidatos, para que o teste falhe hoje (ele
    // seria escolhido apenas se calhar de vir primeiro na lista de entrada)
    // e passe a verde quando o bônus de mustSee existir.
    @Test
    void testeMustSee_shouldBePickedBeforeIdenticalNonMustSeeCandidates() {
        when(prefsRepo.findByUserId(userId)).thenReturn(Optional.of(prefs));
        when(destinationRepo.findById(destinationId)).thenReturn(Optional.of(destination));

        List<Poi> candidates = new ArrayList<>();
        for (int i = 0; i < 3; i++) candidates.add(makePoi(BudgetTier.MODERATE, false));
        Poi mustSeePoi = makePoi(BudgetTier.MODERATE, true);
        candidates.add(mustSeePoi); // deliberadamente não é o primeiro da lista
        for (int i = 0; i < 4; i++) candidates.add(makePoi(BudgetTier.MODERATE, false));

        when(poiRepo.findActiveByDestinationAndBudget(eq(destinationId), anyList()))
            .thenReturn(candidates);

        LocalDate day = LocalDate.now().plusDays(1);
        ItineraryRequest request = new ItineraryRequest(destinationId, day, day, null, null, null, null, null, null);

        ItineraryResponseDTO result = engineService.generate(userId, request);

        UUID firstPickedPoiId = result.days().get(0).activities().get(0).poi().id();

        assertEquals(mustSeePoi.getId(), firstPickedPoiId,
            "O POI mustSee=true deve ser escolhido antes dos demais candidatos idênticos, " +
            "por causa do bônus de pontuação em scoreFor (ainda não implementado)");
    }

    // ─── Regra de negócio nova: bônus de pontuação para o vibe SILENCE ───────
    //
    // Contrato assumido (RED por design — a implementar por um próximo agente
    // em ItineraryEngineService#vibeBonus): com tripVibe=SILENCE, POIs de
    // categoria PARK e MUSEUM devem receber um bônus de pontuação positivo
    // (roteiros de silêncio/tranquilidade priorizam natureza e cultura de
    // baixo fluxo), enquanto NIGHTLIFE e SHOPPING não recebem bônus algum
    // (score 0) — mesmo padrão de "outras categorias -> 0" já usado por
    // CULTURE (que só bonifica MUSEUM) e WELLNESS (PARK/BEACH) hoje.
    //
    // Hoje `case SILENCE -> 0;` foi adicionado ao switch de vibeBonus só para
    // manter o módulo compilando com o novo valor do enum TripVibe — todas as
    // categorias, incluindo PARK e MUSEUM, ainda pontuam 0 sob SILENCE. Por
    // isso, com todos os 8 candidatos empatados em score, a ordenação
    // estável preserva a ordem de entrada da lista (SHOPPING, NIGHTLIFE
    // primeiro), e os dois POIs de PARK/MUSEUM (propositalmente nas posições
    // 3 e 4 da lista de entrada) só seriam escolhidos primeiro quando o
    // bônus de SILENCE existir de verdade.
    @Test
    void testeSilence_shouldPreferParkAndMuseum_overNightlifeAndShopping() {
        when(prefsRepo.findByUserId(userId)).thenReturn(Optional.of(prefs)); // prefs = COUPLE, MODERATE
        when(destinationRepo.findById(destinationId)).thenReturn(Optional.of(destination));

        // COUPLE só recebe groupBonus para RESTAURANT/BEACH/HOTEL (ver
        // ItineraryEngineService#groupBonus), então PARK/MUSEUM/NIGHTLIFE/
        // SHOPPING ficam neutros (0) nesse eixo, isolando o efeito do
        // vibeBonus. Tags vazias evitam o bônus de interesse (+10 por tag
        // batendo com prefs.interests).
        List<Poi> candidates = new ArrayList<>(List.of(
            makePoiWithCategory(PoiCategory.SHOPPING),
            makePoiWithCategory(PoiCategory.NIGHTLIFE),
            makePoiWithCategory(PoiCategory.PARK),    // deliberadamente não é o primeiro da lista
            makePoiWithCategory(PoiCategory.MUSEUM),  // deliberadamente não é o segundo da lista
            makePoiWithCategory(PoiCategory.SHOPPING),
            makePoiWithCategory(PoiCategory.NIGHTLIFE),
            makePoiWithCategory(PoiCategory.SHOPPING),
            makePoiWithCategory(PoiCategory.NIGHTLIFE)
        ));

        when(poiRepo.findActiveByDestinationAndBudget(eq(destinationId), anyList()))
            .thenReturn(candidates);

        LocalDate day = LocalDate.now().plusDays(1);
        ItineraryRequest request = new ItineraryRequest(
            destinationId, day, day, null, TripVibe.SILENCE, null, null, null, null);

        ItineraryResponseDTO result = engineService.generate(userId, request);

        List<PoiCategory> pickedCategories = result.days().get(0).activities().stream()
            .map(a -> a.poi().category())
            .toList();

        assertEquals(PoiCategory.PARK, pickedCategories.get(0),
            "Com tripVibe=SILENCE, o primeiro POI escolhido deve ser de categoria PARK " +
            "(bônus de vibe ainda não implementado em vibeBonus)");
        assertEquals(PoiCategory.MUSEUM, pickedCategories.get(1),
            "Com tripVibe=SILENCE, o segundo POI escolhido deve ser de categoria MUSEUM " +
            "(bônus de vibe ainda não implementado em vibeBonus)");
    }

    // ─── Item A — Regra nova: tripInterests do request sobrepõe onboarding ──
    //
    // Contrato assumido (RED por design — a implementar por um próximo agente
    // em ItineraryEngineService#scoreFor): quando ItineraryRequest.tripInterests()
    // vier não-nulo e não-vazio, ele deve ser usado no lugar de
    // prefs.getInterests() para o bônus de +10 por tag batendo. Hoje scoreFor
    // sempre usa prefs.getInterests(), então um POI cuja tag bate só com
    // tripInterests (e não com os interesses do onboarding) não recebe bônus
    // algum, e um POI com tag do onboarding (mas fora de tripInterests) vence
    // indevidamente — por isso o teste falha até o override ser implementado.
    @Test
    void testeTripInterests_shouldOverrideOnboardingInterests_whenProvided() {
        when(prefsRepo.findByUserId(userId)).thenReturn(Optional.of(prefs)); // prefs.interests = ["cultura", "gastronomia"]
        when(destinationRepo.findById(destinationId)).thenReturn(Optional.of(destination));

        Poi aventuraPoi = makePoiWithTags(List.of("aventura"));
        List<Poi> candidates = new ArrayList<>();
        for (int i = 0; i < 3; i++) candidates.add(makePoiWithTags(List.of("cultura")));
        candidates.add(aventuraPoi); // deliberadamente não é o primeiro da lista
        for (int i = 0; i < 4; i++) candidates.add(makePoiWithTags(List.of("cultura")));

        when(poiRepo.findActiveByDestinationAndBudget(eq(destinationId), anyList()))
            .thenReturn(candidates);

        LocalDate day = LocalDate.now().plusDays(1);
        ItineraryRequest request = new ItineraryRequest(
            destinationId, day, day, null, null, null, null, List.of("aventura"), null);

        ItineraryResponseDTO result = engineService.generate(userId, request);

        UUID firstPickedPoiId = result.days().get(0).activities().get(0).poi().id();

        assertEquals(aventuraPoi.getId(), firstPickedPoiId,
            "Com tripInterests=[\"aventura\"] no request, o POI com tag \"aventura\" deve " +
            "vencer no scoring, mesmo que os interesses do onboarding sejam [\"cultura\", " +
            "\"gastronomia\"] (override de tripInterests ainda não implementado em scoreFor)");
    }

    // ─── Item A — Regra nova: tripInterests=null cai no fallback do onboarding ──

    @Test
    void testeTripInterests_shouldFallBackToOnboardingInterests_whenNull() {
        when(prefsRepo.findByUserId(userId)).thenReturn(Optional.of(prefs)); // prefs.interests = ["cultura", "gastronomia"]
        when(destinationRepo.findById(destinationId)).thenReturn(Optional.of(destination));

        Poi culturaPoi = makePoiWithTags(List.of("cultura"));
        List<Poi> candidates = new ArrayList<>();
        for (int i = 0; i < 3; i++) candidates.add(makePoiWithTags(List.of("aventura")));
        candidates.add(culturaPoi); // deliberadamente não é o primeiro da lista
        for (int i = 0; i < 4; i++) candidates.add(makePoiWithTags(List.of("aventura")));

        when(poiRepo.findActiveByDestinationAndBudget(eq(destinationId), anyList()))
            .thenReturn(candidates);

        LocalDate day = LocalDate.now().plusDays(1);
        ItineraryRequest request = new ItineraryRequest(
            destinationId, day, day, null, null, null, null, null, null);

        ItineraryResponseDTO result = engineService.generate(userId, request);

        UUID firstPickedPoiId = result.days().get(0).activities().get(0).poi().id();

        assertEquals(culturaPoi.getId(), firstPickedPoiId,
            "Com tripInterests=null no request, o scoring deve continuar usando os " +
            "interesses do onboarding (\"cultura\"), como hoje");
    }

    // ─── Item A — Regra nova: tripInterests=[] (vazio) cai no fallback do onboarding ──

    @Test
    void testeTripInterests_shouldFallBackToOnboardingInterests_whenEmpty() {
        when(prefsRepo.findByUserId(userId)).thenReturn(Optional.of(prefs)); // prefs.interests = ["cultura", "gastronomia"]
        when(destinationRepo.findById(destinationId)).thenReturn(Optional.of(destination));

        Poi culturaPoi = makePoiWithTags(List.of("cultura"));
        List<Poi> candidates = new ArrayList<>();
        for (int i = 0; i < 3; i++) candidates.add(makePoiWithTags(List.of("aventura")));
        candidates.add(culturaPoi); // deliberadamente não é o primeiro da lista
        for (int i = 0; i < 4; i++) candidates.add(makePoiWithTags(List.of("aventura")));

        when(poiRepo.findActiveByDestinationAndBudget(eq(destinationId), anyList()))
            .thenReturn(candidates);

        LocalDate day = LocalDate.now().plusDays(1);
        ItineraryRequest request = new ItineraryRequest(
            destinationId, day, day, null, null, null, null, List.of(), null);

        ItineraryResponseDTO result = engineService.generate(userId, request);

        UUID firstPickedPoiId = result.days().get(0).activities().get(0).poi().id();

        assertEquals(culturaPoi.getId(), firstPickedPoiId,
            "Com tripInterests=[] (vazio) no request, o scoring deve continuar usando os " +
            "interesses do onboarding (\"cultura\"), como hoje");
    }

    // ─── Item A — Regra nova: tripRhythm do request sobrepõe onboarding ─────
    //
    // Contrato assumido (RED por design — a implementar por um próximo agente
    // em ItineraryEngineService#generate/slotSpecs): quando
    // ItineraryRequest.tripRhythm() vier não-nulo, ele deve ser usado no lugar
    // de prefs.getRhythm() na distribuição de slots. Hoje slotSpecs sempre
    // recebe prefs.getRhythm(), então mesmo com tripRhythm=INTENSE no
    // request, prefs=MODERATE ainda gera 3 atividades por dia (e não as 4
    // esperadas de INTENSE) — por isso o teste falha até o override ser
    // implementado.
    @Test
    void testeTripRhythm_shouldOverrideOnboardingRhythm_whenProvided() {
        when(prefsRepo.findByUserId(userId)).thenReturn(Optional.of(prefs)); // prefs.rhythm = MODERATE
        when(destinationRepo.findById(destinationId)).thenReturn(Optional.of(destination));
        when(poiRepo.findActiveByDestinationAndBudget(eq(destinationId), anyList()))
            .thenReturn(makePoiList(20, BudgetTier.MODERATE));

        ItineraryRequest request = new ItineraryRequest(
            destinationId, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3),
            null, null, null, null, null, TravelRhythm.INTENSE);

        ItineraryResponseDTO result = engineService.generate(userId, request);

        result.days().forEach(day ->
            assertEquals(4, day.activities().size(),
                "Com tripRhythm=INTENSE no request, cada dia deve ter 4 atividades, " +
                "mesmo que prefs.rhythm seja MODERATE (override ainda não implementado)"));
    }

    // ─── Item A — Regra nova: tripRhythm=null cai no fallback do onboarding ──

    @Test
    void testeTripRhythm_shouldFallBackToOnboardingRhythm_whenNull() {
        OnboardingPreferences intense = prefsWithRhythm(TravelRhythm.INTENSE);
        when(prefsRepo.findByUserId(userId)).thenReturn(Optional.of(intense));
        when(destinationRepo.findById(destinationId)).thenReturn(Optional.of(destination));
        when(poiRepo.findActiveByDestinationAndBudget(eq(destinationId), anyList()))
            .thenReturn(makePoiList(20, BudgetTier.MODERATE));

        ItineraryRequest request = new ItineraryRequest(
            destinationId, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3),
            null, null, null, null, null, null);

        ItineraryResponseDTO result = engineService.generate(userId, request);

        result.days().forEach(day ->
            assertEquals(4, day.activities().size(),
                "Com tripRhythm=null no request, o número de atividades por dia deve " +
                "continuar refletindo prefs.rhythm=INTENSE do onboarding, como hoje"));
    }

    // ─── Regra de negócio nova: usuário EVANGELICAL não recebe igrejas católicas ──
    //
    // Contrato assumido (RED por design — a implementar por um próximo agente
    // em ItineraryEngineService#generate, filtrando `candidates` antes da
    // checagem de InsufficientContentException): quando
    // prefs.getReligion() == Religion.EVANGELICAL, POIs com
    // isCatholicChurch()==true devem ser EXCLUÍDOS totalmente do pool de
    // candidatos — nunca aparecem no roteiro gerado, igual a um POI fora do
    // budget. Hoje o filtro não existe, então com 8 POIs não-católicos e 3
    // POIs de igreja católica (11 no total) distribuídos em 3 dias de ritmo
    // MODERATE (9 usos de slot ao todo, mais que os 8 POIs não-católicos
    // disponíveis), o motor é forçado a reaproveitar/alcançar os POIs
    // católicos no ranking (que vêm depois na lista de entrada, mas o pool
    // não-católico se esgota antes do fim dos 9 slots) — por isso o teste
    // falha hoje.
    @Test
    void testeEvangelical_shouldExcludeCatholicChurchPois_fromGeneratedItinerary() {
        OnboardingPreferences evangelical = prefsWithReligion(Religion.EVANGELICAL);
        when(prefsRepo.findByUserId(userId)).thenReturn(Optional.of(evangelical));
        when(destinationRepo.findById(destinationId)).thenReturn(Optional.of(destination));

        List<Poi> nonCatholic = new ArrayList<>();
        for (int i = 0; i < 8; i++) nonCatholic.add(makePoi(BudgetTier.MODERATE, false, false));
        List<Poi> catholicChurches = new ArrayList<>();
        for (int i = 0; i < 3; i++) catholicChurches.add(makePoi(BudgetTier.MODERATE, false, true));
        Set<UUID> catholicIds = catholicChurches.stream().map(Poi::getId).collect(java.util.stream.Collectors.toSet());

        List<Poi> candidates = new ArrayList<>(nonCatholic);
        candidates.addAll(catholicChurches);

        when(poiRepo.findActiveByDestinationAndBudget(eq(destinationId), anyList()))
            .thenReturn(candidates);

        ItineraryRequest request = new ItineraryRequest(
            destinationId, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3),
            null, null, null, null, null, null);

        ItineraryResponseDTO result = engineService.generate(userId, request);

        List<UUID> allPickedIds = result.days().stream()
            .flatMap(d -> d.activities().stream())
            .map(a -> a.poi().id())
            .toList();

        assertTrue(allPickedIds.stream().noneMatch(catholicIds::contains),
            "Usuário EVANGELICAL não deve receber nenhum POI de igreja católica no roteiro " +
            "(exclusão total ainda não implementada em ItineraryEngineService#generate)");
    }

    // ─── Regra de negócio nova: exclusão de igrejas católicas pode disparar
    //     InsufficientContentException para usuário EVANGELICAL ──────────────
    //
    // Contrato assumido (RED por design): a exclusão de POIs católicos deve
    // acontecer ANTES da checagem `candidates.size() < 8`. Aqui há 9
    // candidatos no total (>= 8, então hoje NÃO dispara a exceção), mas
    // apenas 5 deles não são igrejas católicas — depois da exclusão restam
    // 5 < 8, então a exceção deveria disparar. Hoje isso não acontece porque
    // o filtro de religião ainda não existe.
    @Test
    void testeEvangelical_shouldThrowInsufficientContent_whenFewerThan8AfterExcludingCatholicChurches() {
        OnboardingPreferences evangelical = prefsWithReligion(Religion.EVANGELICAL);
        when(prefsRepo.findByUserId(userId)).thenReturn(Optional.of(evangelical));
        when(destinationRepo.findById(destinationId)).thenReturn(Optional.of(destination));

        List<Poi> candidates = new ArrayList<>();
        for (int i = 0; i < 5; i++) candidates.add(makePoi(BudgetTier.MODERATE, false, false));
        for (int i = 0; i < 4; i++) candidates.add(makePoi(BudgetTier.MODERATE, false, true));

        when(poiRepo.findActiveByDestinationAndBudget(eq(destinationId), anyList()))
            .thenReturn(candidates);

        ItineraryRequest request = new ItineraryRequest(
            destinationId, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3),
            null, null, null, null, null, null);

        assertThrows(InsufficientContentException.class,
            () -> engineService.generate(userId, request),
            "Com apenas 5 POIs elegíveis depois de excluir as igrejas católicas (de 9 " +
            "candidatos totais), deve lançar InsufficientContentException para usuário " +
            "EVANGELICAL (filtro de religião ainda não implementado)");
    }

    // ─── Regressão: usuário não-EVANGELICAL continua elegível para igrejas
    //     católicas ────────────────────────────────────────────────────────
    //
    // Comportamento atual (sem filtro algum) já satisfaz este teste, e deve
    // continuar satisfazendo depois que o filtro de EVANGELICAL for
    // implementado, já que CATHOLIC não deve ser afetado pela nova regra.
    @Test
    void testeCatholic_shouldStillIncludeCatholicChurchPois_inGeneratedItinerary() {
        OnboardingPreferences catholic = prefsWithReligion(Religion.CATHOLIC);
        when(prefsRepo.findByUserId(userId)).thenReturn(Optional.of(catholic));
        when(destinationRepo.findById(destinationId)).thenReturn(Optional.of(destination));

        List<Poi> nonCatholic = new ArrayList<>();
        for (int i = 0; i < 5; i++) nonCatholic.add(makePoi(BudgetTier.MODERATE, false, false));
        List<Poi> catholicChurches = new ArrayList<>();
        for (int i = 0; i < 3; i++) catholicChurches.add(makePoi(BudgetTier.MODERATE, false, true));
        Set<UUID> catholicIds = catholicChurches.stream().map(Poi::getId).collect(java.util.stream.Collectors.toSet());

        List<Poi> candidates = new ArrayList<>(nonCatholic);
        candidates.addAll(catholicChurches);

        when(poiRepo.findActiveByDestinationAndBudget(eq(destinationId), anyList()))
            .thenReturn(candidates);

        // 3 dias de ritmo MODERATE (prefs padrão) = 9 usos de slot no total,
        // mais que os 8 candidatos disponíveis, forçando reaproveitamento e
        // garantindo que todos os 8 POIs (incluindo as 3 igrejas católicas)
        // sejam usados em algum momento do roteiro.
        ItineraryRequest request = new ItineraryRequest(
            destinationId, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3),
            null, null, null, null, null, null);

        ItineraryResponseDTO result = engineService.generate(userId, request);

        List<UUID> allPickedIds = result.days().stream()
            .flatMap(d -> d.activities().stream())
            .map(a -> a.poi().id())
            .toList();

        assertTrue(allPickedIds.stream().anyMatch(catholicIds::contains),
            "Usuário CATHOLIC deve continuar podendo receber POIs de igreja católica no " +
            "roteiro (comportamento não deve mudar para religiões diferentes de EVANGELICAL)");
    }

    // ─── Regra de negócio nova: priorização por proximidade ao stayArea ─────
    //
    // Contrato: quando ItineraryRequest.stayArea() vier null/blank, o
    // motor NUNCA deve chamar geocodingService.geocode(...) (não há o que
    // geocodificar).

    @Test
    void testeStayArea_shouldNeverCallGeocoding_whenStayAreaIsNullOrBlank() {
        when(prefsRepo.findByUserId(userId)).thenReturn(Optional.of(prefs));
        when(destinationRepo.findById(destinationId)).thenReturn(Optional.of(destination));
        when(poiRepo.findActiveByDestinationAndBudget(eq(destinationId), anyList()))
            .thenReturn(makePoiList(10, BudgetTier.MODERATE));

        LocalDate day = LocalDate.now().plusDays(1);
        ItineraryRequest requestNullStayArea = new ItineraryRequest(
            destinationId, day, day, null, null, null, null, null, null);
        engineService.generate(userId, requestNullStayArea);

        ItineraryRequest requestBlankStayArea = new ItineraryRequest(
            destinationId, day, day, "   ", null, null, null, null, null);
        engineService.generate(userId, requestBlankStayArea);

        verify(geocodingService, never()).geocode(anyString());
    }

    // ─── Regra de negócio nova: POI mais próximo do stayArea é priorizado ───
    //
    // Contrato: quando stayArea vier preenchido e o geocoding resolver uma
    // localização, um POI a <=1km dessa localização recebe bônus de
    // pontuação (+6) e deve ser priorizado sobre um POI idêntico em tudo
    // mais, porém distante (>6km, sem bônus). Categoria MUSEUM não recebe
    // groupBonus para COUPLE nem vibeBonus sem tripVibe, e tags vazias
    // evitam o bônus de interesse — isolando o efeito do bônus de
    // proximidade.
    @Test
    void testeStayArea_shouldPrioritizeNearestPoi_whenGeocodingResolvesLocation() {
        when(prefsRepo.findByUserId(userId)).thenReturn(Optional.of(prefs));
        when(destinationRepo.findById(destinationId)).thenReturn(Optional.of(destination));

        LatLng stayAreaLatLng = new LatLng(48.8600, 2.3500);
        when(geocodingService.geocode(anyString())).thenReturn(Optional.of(stayAreaLatLng));

        Poi nearPoi = makePoiAt(BigDecimal.valueOf(48.8601), BigDecimal.valueOf(2.3501), false);
        Poi farPoi = makePoiAt(BigDecimal.valueOf(49.9000), BigDecimal.valueOf(3.4000), false);

        List<Poi> candidates = new ArrayList<>();
        candidates.add(farPoi);
        for (int i = 0; i < 6; i++) {
            candidates.add(makePoiAt(BigDecimal.valueOf(49.9000 + i * 0.01), BigDecimal.valueOf(3.4000), false));
        }
        candidates.add(nearPoi); // deliberadamente não é o primeiro da lista

        when(poiRepo.findActiveByDestinationAndBudget(eq(destinationId), anyList()))
            .thenReturn(candidates);

        LocalDate day = LocalDate.now().plusDays(1);
        ItineraryRequest request = new ItineraryRequest(
            destinationId, day, day, "Le Marais", null, null, null, null, null);

        ItineraryResponseDTO result = engineService.generate(userId, request);

        UUID firstPickedPoiId = result.days().get(0).activities().get(0).poi().id();

        assertEquals(nearPoi.getId(), firstPickedPoiId,
            "O POI mais próximo do stayArea geocodificado deve ser priorizado sobre " +
            "candidatos idênticos, porém distantes (bônus de proximidade ainda não " +
            "implementado em scoreFor)");
    }

    // ─── Regra de negócio nova: bônus de proximidade nunca supera mustSee ───
    //
    // Contrato: o bônus de proximidade (+6 no máximo) é PROPOSITALMENTE
    // menor que o bônus de mustSee (+20) — um POI mustSee=true longe do
    // stayArea ainda deve pontuar mais que um POI comum perto do stayArea.
    @Test
    void testeStayArea_mustSeeBonusShouldOutweighProximityBonus() {
        when(prefsRepo.findByUserId(userId)).thenReturn(Optional.of(prefs));
        when(destinationRepo.findById(destinationId)).thenReturn(Optional.of(destination));

        LatLng stayAreaLatLng = new LatLng(48.8600, 2.3500);
        when(geocodingService.geocode(anyString())).thenReturn(Optional.of(stayAreaLatLng));

        Poi mustSeeFarPoi = makePoiAt(BigDecimal.valueOf(49.9000), BigDecimal.valueOf(3.4000), true);
        Poi commonNearPoi = makePoiAt(BigDecimal.valueOf(48.8601), BigDecimal.valueOf(2.3501), false);

        List<Poi> candidates = new ArrayList<>();
        candidates.add(commonNearPoi);
        for (int i = 0; i < 6; i++) {
            candidates.add(makePoiAt(BigDecimal.valueOf(49.9000 + i * 0.01), BigDecimal.valueOf(3.4000), false));
        }
        candidates.add(mustSeeFarPoi); // deliberadamente não é o primeiro da lista

        when(poiRepo.findActiveByDestinationAndBudget(eq(destinationId), anyList()))
            .thenReturn(candidates);

        LocalDate day = LocalDate.now().plusDays(1);
        ItineraryRequest request = new ItineraryRequest(
            destinationId, day, day, "Le Marais", null, null, null, null, null);

        ItineraryResponseDTO result = engineService.generate(userId, request);

        UUID firstPickedPoiId = result.days().get(0).activities().get(0).poi().id();

        assertEquals(mustSeeFarPoi.getId(), firstPickedPoiId,
            "O bônus de mustSee (+20) deve superar o bônus de proximidade (+6 no máximo), " +
            "mesmo quando o POI mustSee está longe do stayArea e o POI comum está perto");
    }

    // ─── Regra de negócio nova: variedade ao longo da viagem (TripSaturationLedger) ──
    //
    // Contrato (RED por design — a implementar em ItineraryEngineService
    // via TripSaturationLedger): categoria repetida demais na viagem perde
    // pontos aos poucos, e depois que a viagem fica saturada de mustSee
    // (>50% das atividades já escolhidas, com amostra mínima de 4), POIs
    // não-mustSee ganham um empurrão de pontuação — nunca o suficiente pra
    // superar um mustSee "limpo", mas o bastante pra virar o jogo quando o
    // mustSee concorrente já está com a própria categoria penalizada.

    @Test
    void testeVariedade_shouldPenalizeRepeatedCategory_favoringFreshCategoryOnNextPick() {
        // SOLO neutro pra RESTAURANT e PARK (ambos 0 em groupBonus — SOLO só
        // bonifica MUSEUM/NIGHTLIFE), sem interesses, sem vibe — isola o
        // efeito da penalidade de categoria de qualquer outro bônus.
        OnboardingPreferences neutralPrefs = OnboardingPreferences.builder()
            .id(UUID.randomUUID()).user(user)
            .budget(BudgetTier.MODERATE).rhythm(TravelRhythm.RELAXED)
            .groupType(GroupType.SOLO).interests(List.of())
            .build();
        when(prefsRepo.findByUserId(userId)).thenReturn(Optional.of(neutralPrefs));
        when(destinationRepo.findById(destinationId)).thenReturn(Optional.of(destination));

        List<Poi> candidates = new ArrayList<>();
        Poi restaurant0 = makePoiWithCategory(PoiCategory.RESTAURANT);
        Poi restaurant1 = makePoiWithCategory(PoiCategory.RESTAURANT);
        Poi park = makePoiWithCategory(PoiCategory.PARK);
        candidates.add(restaurant0);
        candidates.add(restaurant1);
        candidates.add(park);
        for (int i = 0; i < 5; i++) candidates.add(makePoiWithCategory(PoiCategory.SHOPPING)); // filler, >= 8 no total

        when(poiRepo.findActiveByDestinationAndBudget(eq(destinationId), anyList()))
            .thenReturn(candidates);

        LocalDate start = LocalDate.now().plusDays(1);
        ItineraryRequest request = new ItineraryRequest(
            destinationId, start, start, null, null, null, null, null, null); // RELAXED, 1 dia = 2 slots (Morning + Afternoon)

        ItineraryResponseDTO result = engineService.generate(userId, request);

        List<UUID> pickedIds = result.days().get(0).activities().stream().map(a -> a.poi().id()).toList();

        assertEquals(restaurant0.getId(), pickedIds.get(0),
            "Primeiro pick: tudo empatado em 0, desempate por ordem de entrada favorece o primeiro RESTAURANT");
        assertEquals(park.getId(), pickedIds.get(1),
            "Segundo pick: o segundo RESTAURANT já carrega penalidade de categoria (-2) e perde pro PARK " +
            "(categoria ainda não usada, score 0) — penalidade de repetição ainda não implementada em scoreFor");
    }

    @Test
    void testeVariedade_shouldBoostNonMustSeeCategory_afterTripBecomesMustSeeSaturated() {
        // GROUP type: MUSEUM e RESTAURANT ficam neutros (0 em groupBonus),
        // só "gastronomia" dá bônus de interesse — isola mustSee + penalidade
        // de categoria + bônus de saturação.
        OnboardingPreferences prefsGastronomia = OnboardingPreferences.builder()
            .id(UUID.randomUUID()).user(user)
            .budget(BudgetTier.MODERATE).rhythm(TravelRhythm.MODERATE)
            .groupType(GroupType.GROUP).interests(List.of("gastronomia"))
            .build();
        when(prefsRepo.findByUserId(userId)).thenReturn(Optional.of(prefsGastronomia));
        when(destinationRepo.findById(destinationId)).thenReturn(Optional.of(destination));

        List<Poi> candidates = new ArrayList<>();
        // 5 MUSEUM mustSee idênticos: 4 serão consumidos no dia 1 (3 slots) +
        // manhã do dia 2, sobrando 1 ("lateMustSee") pra concorrer na tarde
        // do dia 2 contra o POI nichado.
        List<Poi> museumsMustSee = new ArrayList<>();
        for (int i = 0; i < 5; i++) museumsMustSee.add(makePoiWithCategoryMustSeeAndTags(PoiCategory.MUSEUM, true, List.of()));
        candidates.addAll(museumsMustSee);

        Poi nicheCandidate = makePoiWithCategoryMustSeeAndTags(PoiCategory.RESTAURANT, false, List.of("gastronomia"));
        candidates.add(nicheCandidate);

        for (int i = 0; i < 2; i++) candidates.add(makePoiWithCategory(PoiCategory.SHOPPING)); // filler, >= 8 no total

        when(poiRepo.findActiveByDestinationAndBudget(eq(destinationId), anyList()))
            .thenReturn(candidates);

        LocalDate start = LocalDate.now().plusDays(1);
        ItineraryRequest request = new ItineraryRequest(
            destinationId, start, start.plusDays(1), null, null, null, null, null, null); // MODERATE = 3 slots/dia, 2 dias

        ItineraryResponseDTO result = engineService.generate(userId, request);

        // Dia 2, tarde (2º slot do dia 2) = 5º pick no total — ledger nesse
        // ponto: 4 MUSEUM mustSee já escolhidos (dia 1 inteiro + manhã do
        // dia 2), então totalCount=4, mustSeeCount=4 (saturado). O MUSEUM
        // restante pontua 20 (mustSee) - 8 (penalidade, 4 ocorrências) = 12.
        // O nicheCandidate pontua 0 (groupBonus) + 10 (tag "gastronomia") +
        // 8 (bônus de saturação, categoria RESTAURANT ainda fresca) = 18.
        // 18 > 12 — sem o bônus de saturação, o MUSEUM (12) venceria o
        // nicheCandidate (10).
        UUID afternoonDay2PoiId = result.days().get(1).activities().get(1).poi().id();

        assertEquals(nicheCandidate.getId(), afternoonDay2PoiId,
            "Depois que a viagem fica saturada de mustSee, o POI não-mustSee de categoria fresca deve " +
            "ser priorizado sobre mais um mustSee de categoria já penalizada (bônus de saturação ainda " +
            "não implementado em scoreFor/TripSaturationLedger)");
    }

    // ─── Dia inteiro (fullDayActivity) ─────────────────────────────────────

    @Test
    void testeFullDay_shouldBecomeTheOnlyActivityOfTheDay_whenAFullDayPoiIsAvailable() {
        when(prefsRepo.findByUserId(userId)).thenReturn(Optional.of(prefs));
        when(destinationRepo.findById(destinationId)).thenReturn(Optional.of(destination));

        Poi themePark = makeFullDayPoi();
        List<Poi> candidates = new ArrayList<>(makePoiList(10, BudgetTier.MODERATE));
        candidates.add(themePark);
        when(poiRepo.findActiveByDestinationAndBudget(eq(destinationId), anyList()))
            .thenReturn(candidates);

        LocalDate start = LocalDate.now().plusDays(1);
        ItineraryRequest request = new ItineraryRequest(
            destinationId, start, start, null, null, null, null, null, null); // 1 dia, MODERATE (normalmente 3 slots)

        ItineraryResponseDTO result = engineService.generate(userId, request);

        List<luvoygroup.voyage.features.itinerary.dto.ActivityDTO> activities = result.days().get(0).activities();
        assertEquals(1, activities.size(),
            "Dia com POI de dia inteiro disponível deve ter 1 única atividade, não os 3 slots normais");
        assertEquals(ItinerarySlot.FULL_DAY, activities.get(0).slot());
        assertEquals(themePark.getId(), activities.get(0).poi().id());
    }

    @Test
    void testeFullDay_shouldNeverLeakIntoRegularSlots_onOtherDays_onceAlreadyUsed() {
        when(prefsRepo.findByUserId(userId)).thenReturn(Optional.of(prefs));
        when(destinationRepo.findById(destinationId)).thenReturn(Optional.of(destination));

        Poi themePark = makeFullDayPoi();
        List<Poi> candidates = new ArrayList<>(makePoiList(10, BudgetTier.MODERATE));
        candidates.add(themePark);
        when(poiRepo.findActiveByDestinationAndBudget(eq(destinationId), anyList()))
            .thenReturn(candidates);

        LocalDate start = LocalDate.now().plusDays(1);
        ItineraryRequest request = new ItineraryRequest(
            destinationId, start, start.plusDays(1), null, null, null, null, null, null); // 2 dias, MODERATE

        ItineraryResponseDTO result = engineService.generate(userId, request);

        // Dia 1 consome o POI de dia inteiro; dia 2 volta ao fluxo normal de
        // 3 slots, e o POI de dia inteiro nunca deve reaparecer (nem no
        // segundo passe de reuso de pickPois, que existe pra viagens longas).
        assertEquals(1, result.days().get(0).activities().size());
        assertEquals(ItinerarySlot.FULL_DAY, result.days().get(0).activities().get(0).slot());

        List<luvoygroup.voyage.features.itinerary.dto.ActivityDTO> day2Activities = result.days().get(1).activities();
        assertEquals(3, day2Activities.size(), "Dia 2 sem POI de dia inteiro disponível segue o fluxo normal (3 slots)");
        assertTrue(day2Activities.stream().noneMatch(a -> a.poi().id().equals(themePark.getId())),
            "POI de dia inteiro já usado não pode reaparecer num slot normal (manhã/tarde/noite)");
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private Poi makePoiWithCategoryMustSeeAndTags(PoiCategory category, boolean mustSee, List<String> tags) {
        return Poi.builder()
            .id(UUID.randomUUID())
            .destination(destination)
            .title("POI-" + UUID.randomUUID().toString().substring(0, 6))
            .category(category)
            .budgetTier(BudgetTier.MODERATE)
            .tags(tags)
            .active(true)
            .mustSee(mustSee)
            .latitude(BigDecimal.valueOf(48.8 + Math.random() * 0.1))
            .longitude(BigDecimal.valueOf(2.3 + Math.random() * 0.1))
            .averageDurationMinutes(90)
            .build();
    }



    private Poi makePoi(BudgetTier tier) {
        return makePoi(tier, false);
    }

    private Poi makePoiWithTags(List<String> tags) {
        return Poi.builder()
            .id(UUID.randomUUID())
            .destination(destination)
            .title("POI-" + UUID.randomUUID().toString().substring(0, 6))
            .category(PoiCategory.MUSEUM)
            .budgetTier(BudgetTier.MODERATE)
            .tags(tags)
            .active(true)
            .mustSee(false)
            .latitude(BigDecimal.valueOf(48.8 + Math.random() * 0.1))
            .longitude(BigDecimal.valueOf(2.3 + Math.random() * 0.1))
            .averageDurationMinutes(90)
            .build();
    }

    private Poi makePoiAt(BigDecimal latitude, BigDecimal longitude, boolean mustSee) {
        return Poi.builder()
            .id(UUID.randomUUID())
            .destination(destination)
            .title("POI-" + UUID.randomUUID().toString().substring(0, 6))
            .category(PoiCategory.MUSEUM)
            .budgetTier(BudgetTier.MODERATE)
            .tags(List.of())
            .active(true)
            .mustSee(mustSee)
            .latitude(latitude)
            .longitude(longitude)
            .averageDurationMinutes(90)
            .build();
    }

    private Poi makePoiWithCategory(PoiCategory category) {
        return Poi.builder()
            .id(UUID.randomUUID())
            .destination(destination)
            .title("POI-" + UUID.randomUUID().toString().substring(0, 6))
            .category(category)
            .budgetTier(BudgetTier.MODERATE)
            .tags(List.of())
            .active(true)
            .mustSee(false)
            .latitude(BigDecimal.valueOf(48.8 + Math.random() * 0.1))
            .longitude(BigDecimal.valueOf(2.3 + Math.random() * 0.1))
            .averageDurationMinutes(90)
            .build();
    }

    private Poi makePoi(BudgetTier tier, boolean mustSee) {
        return makePoi(tier, mustSee, false);
    }

    private Poi makePoi(BudgetTier tier, boolean mustSee, boolean catholicChurch) {
        return Poi.builder()
            .id(UUID.randomUUID())
            .destination(destination)
            .title("POI-" + UUID.randomUUID().toString().substring(0, 6))
            .category(PoiCategory.MUSEUM)
            .budgetTier(tier)
            .tags(List.of("cultura"))
            .active(true)
            .mustSee(mustSee)
            .catholicChurch(catholicChurch)
            .latitude(BigDecimal.valueOf(48.8 + Math.random() * 0.1))
            .longitude(BigDecimal.valueOf(2.3 + Math.random() * 0.1))
            .averageDurationMinutes(90)
            .build();
    }

    private Poi makeFullDayPoi() {
        return Poi.builder()
            .id(UUID.randomUUID())
            .destination(destination)
            .title("Parque Temático de Teste")
            .category(PoiCategory.PARK)
            .budgetTier(BudgetTier.MODERATE)
            .tags(List.of())
            .active(true)
            .fullDayActivity(true)
            .latitude(BigDecimal.valueOf(48.8))
            .longitude(BigDecimal.valueOf(2.3))
            .averageDurationMinutes(480)
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

    private OnboardingPreferences prefsWithReligion(Religion religion) {
        return OnboardingPreferences.builder()
            .id(UUID.randomUUID()).user(user)
            .budget(BudgetTier.MODERATE).rhythm(TravelRhythm.MODERATE)
            .groupType(GroupType.COUPLE).interests(List.of("cultura"))
            .religion(religion)
            .build();
    }
}
