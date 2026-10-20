package luvoygroup.voyage.features.itinerary;

import luvoygroup.voyage.domain.Destination;
import luvoygroup.voyage.domain.Itinerary;
import luvoygroup.voyage.domain.User;
import luvoygroup.voyage.exception.InvalidDateRangeException;
import luvoygroup.voyage.exception.PaymentRequiredException;
import luvoygroup.voyage.exception.ResourceNotFoundException;
import luvoygroup.voyage.features.itinerary.dto.*;
import luvoygroup.voyage.repository.ItineraryActivityRepository;
import luvoygroup.voyage.repository.ItineraryRepository;
import luvoygroup.voyage.repository.PoiRepository;
import luvoygroup.voyage.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Testes unitários "standalone" de ItineraryService — mesmo padrão
 * estabelecido em AdminServiceTest/LlmServiceTest: NÃO usa
 * @ExtendWith(MockitoExtension.class) de propósito (mocks via
 * Mockito.mock() direto), service instanciado via construtor gerado pelo
 * Lombok (@RequiredArgsConstructor).
 *
 * Cobre a feature multi-destino (item 7 do backlog, ver luvoy-pendencias.md):
 * ItineraryEngineService é mockado, então a lógica real de geração de
 * atividades não é exercitada aqui — só a orquestração de
 * generateTrip/findAllTrips/findTripByGroupId (agrupamento por
 * tripGroupId, validação de sobreposição de datas).
 */
class ItineraryServiceTest {

    private ItineraryEngineService engineService;
    private ItineraryRepository itineraryRepo;
    private ItineraryActivityRepository activityRepo;
    private PoiRepository poiRepo;
    private UserRepository userRepo;

    private ItineraryService service;

    private static final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        engineService = mock(ItineraryEngineService.class);
        itineraryRepo = mock(ItineraryRepository.class);
        activityRepo = mock(ItineraryActivityRepository.class);
        poiRepo = mock(PoiRepository.class);
        userRepo = mock(UserRepository.class);

        service = new ItineraryService(engineService, itineraryRepo, activityRepo, poiRepo, userRepo);

        // Free por padrão, sem nenhum roteiro este mês — a maioria dos
        // testes existentes não se importa com o limite mensal; os testes
        // específicos da regra sobrescrevem isso.
        when(userRepo.findById(USER_ID)).thenReturn(Optional.of(freeUser()));
        when(itineraryRepo.findAllByUserIdAndDeletedAtIsNull(USER_ID)).thenReturn(List.of());
    }

    private User freeUser() {
        return User.builder().id(USER_ID).isPremium(false).build();
    }

    private User premiumUser() {
        return User.builder().id(USER_ID).isPremium(true).build();
    }

    private Destination destinationWith(String name) {
        return Destination.builder().id(UUID.randomUUID()).slug(name.toLowerCase()).name(name)
            .country("País").city(name).tagline("").active(true).build();
    }

    private Itinerary itineraryWith(UUID id, Destination destination, LocalDate start, LocalDate end, String title) {
        return Itinerary.builder()
            .id(id).user(null).destination(destination)
            .startDate(start).endDate(end).title(title)
            .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
            .build();
    }

    private ItineraryRequest requestFor(UUID destinationId, LocalDate start, LocalDate end) {
        return new ItineraryRequest(destinationId, start, end, null, null, null, null, null, null);
    }

    // ─── generateTrip ───────────────────────────────────────────────────────

    @Test
    void generateTrip_shouldCreateAllLegs_withSharedTripGroupId_andSequentialOrder() {
        Destination paris = destinationWith("Paris");
        Destination amsterdam = destinationWith("Amsterdã");

        UUID legIdParis = UUID.randomUUID();
        UUID legIdAmsterdam = UUID.randomUUID();

        ItineraryRequest legParis = requestFor(paris.getId(), LocalDate.of(2027, 9, 1), LocalDate.of(2027, 9, 4));
        ItineraryRequest legAmsterdam = requestFor(amsterdam.getId(), LocalDate.of(2027, 9, 5), LocalDate.of(2027, 9, 7));

        Itinerary itineraryParis = itineraryWith(legIdParis, paris, legParis.startDate(), legParis.endDate(), "Paris");
        Itinerary itineraryAmsterdam = itineraryWith(legIdAmsterdam, amsterdam, legAmsterdam.startDate(), legAmsterdam.endDate(), "Amsterdã");

        when(engineService.generate(eq(USER_ID), eq(legParis), any(TripSaturationLedger.class)))
            .thenReturn(new ItineraryResponseDTO(legIdParis, "Paris", paris.getId(), "Paris", "", "Teste", legParis.startDate(), legParis.endDate(), List.of()));
        when(engineService.generate(eq(USER_ID), eq(legAmsterdam), any(TripSaturationLedger.class)))
            .thenReturn(new ItineraryResponseDTO(legIdAmsterdam, "Amsterdã", amsterdam.getId(), "Amsterdã", "", "Teste", legAmsterdam.startDate(), legAmsterdam.endDate(), List.of()));

        when(itineraryRepo.findById(legIdParis)).thenReturn(Optional.of(itineraryParis));
        when(itineraryRepo.findById(legIdAmsterdam)).thenReturn(Optional.of(itineraryAmsterdam));
        when(itineraryRepo.save(any(Itinerary.class))).thenAnswer(inv -> inv.getArgument(0));

        TripSummaryDTO trip = service.generateTrip(USER_ID, new CreateTripRequest(List.of(legParis, legAmsterdam)));

        assertNotNull(trip.tripGroupId());
        assertEquals(2, trip.legs().size());
        assertEquals("Paris + Amsterdã", trip.title());
        assertEquals(LocalDate.of(2027, 9, 1), trip.startDate());
        assertEquals(LocalDate.of(2027, 9, 7), trip.endDate());

        assertEquals(itineraryParis.getTripGroupId(), itineraryAmsterdam.getTripGroupId());
        assertEquals(0, itineraryParis.getTripGroupOrder());
        assertEquals(1, itineraryAmsterdam.getTripGroupOrder());

        verify(itineraryRepo).save(itineraryParis);
        verify(itineraryRepo).save(itineraryAmsterdam);
    }

    @Test
    void generateTrip_shouldAllowGapBetweenLegs() {
        Destination paris = destinationWith("Paris");
        Destination roma = destinationWith("Roma");

        // Trecho 1 termina dia 4, trecho 2 só começa dia 6 (folga de 1 dia) — permitido.
        ItineraryRequest legParis = requestFor(paris.getId(), LocalDate.of(2027, 9, 1), LocalDate.of(2027, 9, 4));
        ItineraryRequest legRoma = requestFor(roma.getId(), LocalDate.of(2027, 9, 6), LocalDate.of(2027, 9, 9));

        UUID legIdParis = UUID.randomUUID();
        UUID legIdRoma = UUID.randomUUID();
        Itinerary itineraryParis = itineraryWith(legIdParis, paris, legParis.startDate(), legParis.endDate(), "Paris");
        Itinerary itineraryRoma = itineraryWith(legIdRoma, roma, legRoma.startDate(), legRoma.endDate(), "Roma");

        when(engineService.generate(eq(USER_ID), eq(legParis), any(TripSaturationLedger.class)))
            .thenReturn(new ItineraryResponseDTO(legIdParis, "Paris", paris.getId(), "Paris", "", "Teste", legParis.startDate(), legParis.endDate(), List.of()));
        when(engineService.generate(eq(USER_ID), eq(legRoma), any(TripSaturationLedger.class)))
            .thenReturn(new ItineraryResponseDTO(legIdRoma, "Roma", roma.getId(), "Roma", "", "Teste", legRoma.startDate(), legRoma.endDate(), List.of()));
        when(itineraryRepo.findById(legIdParis)).thenReturn(Optional.of(itineraryParis));
        when(itineraryRepo.findById(legIdRoma)).thenReturn(Optional.of(itineraryRoma));
        when(itineraryRepo.save(any(Itinerary.class))).thenAnswer(inv -> inv.getArgument(0));

        TripSummaryDTO trip = service.generateTrip(USER_ID, new CreateTripRequest(List.of(legParis, legRoma)));

        assertEquals(2, trip.legs().size());
        verify(engineService).generate(eq(USER_ID), eq(legParis), any(TripSaturationLedger.class));
        verify(engineService).generate(eq(USER_ID), eq(legRoma), any(TripSaturationLedger.class));
    }

    @Test
    void generateTrip_shouldThrowInvalidDateRange_whenLegsOverlap() {
        UUID parisId = UUID.randomUUID();
        UUID romaId = UUID.randomUUID();

        // Trecho 2 começa (dia 3) antes do trecho 1 terminar (dia 4) — sobreposição proibida.
        ItineraryRequest legParis = requestFor(parisId, LocalDate.of(2027, 9, 1), LocalDate.of(2027, 9, 4));
        ItineraryRequest legRoma = requestFor(romaId, LocalDate.of(2027, 9, 3), LocalDate.of(2027, 9, 6));

        assertThrows(InvalidDateRangeException.class,
            () -> service.generateTrip(USER_ID, new CreateTripRequest(List.of(legParis, legRoma))));

        verifyNoInteractions(engineService);
    }

    @Test
    void generateTrip_shouldThrowInvalidDateRange_whenLegsShareTheSameDay() {
        UUID parisId = UUID.randomUUID();
        UUID romaId = UUID.randomUUID();

        // Trecho 2 começa NO MESMO dia em que o trecho 1 termina — também é sobreposição.
        ItineraryRequest legParis = requestFor(parisId, LocalDate.of(2027, 9, 1), LocalDate.of(2027, 9, 4));
        ItineraryRequest legRoma = requestFor(romaId, LocalDate.of(2027, 9, 4), LocalDate.of(2027, 9, 6));

        assertThrows(InvalidDateRangeException.class,
            () -> service.generateTrip(USER_ID, new CreateTripRequest(List.of(legParis, legRoma))));
    }

    // ─── findAllTrips ───────────────────────────────────────────────────────

    @Test
    void findAllTrips_shouldGroupLegsSharingTheSameTripGroupId() {
        UUID tripGroupId = UUID.randomUUID();
        Destination paris = destinationWith("Paris");
        Destination amsterdam = destinationWith("Amsterdã");

        Itinerary legAmsterdam = itineraryWith(UUID.randomUUID(), amsterdam,
            LocalDate.of(2027, 9, 5), LocalDate.of(2027, 9, 7), "Amsterdã");
        legAmsterdam.setTripGroupId(tripGroupId);
        legAmsterdam.setTripGroupOrder(1);

        Itinerary legParis = itineraryWith(UUID.randomUUID(), paris,
            LocalDate.of(2027, 9, 1), LocalDate.of(2027, 9, 4), "Paris");
        legParis.setTripGroupId(tripGroupId);
        legParis.setTripGroupOrder(0);

        // Propositalmente fora de ordem na lista retornada pelo repo — a
        // ordenação por tripGroupOrder deve corrigir isso no resultado.
        when(itineraryRepo.findAllByUserIdAndDeletedAtIsNull(USER_ID)).thenReturn(List.of(legAmsterdam, legParis));

        List<TripSummaryDTO> trips = service.findAllTrips(USER_ID);

        assertEquals(1, trips.size());
        TripSummaryDTO trip = trips.get(0);
        assertEquals(tripGroupId, trip.tripGroupId());
        assertEquals(2, trip.legs().size());
        assertEquals("Paris", trip.legs().get(0).destinationName());
        assertEquals("Amsterdã", trip.legs().get(1).destinationName());
    }

    @Test
    void findAllTrips_shouldTreatStandaloneItinerary_asSingleLegGroup_withNullTripGroupId() {
        Destination lisboa = destinationWith("Lisboa");
        Itinerary avulso = itineraryWith(UUID.randomUUID(), lisboa,
            LocalDate.of(2027, 10, 1), LocalDate.of(2027, 10, 5), "Lisboa");
        // tripGroupId/tripGroupOrder ficam null — roteiro avulso normal.

        when(itineraryRepo.findAllByUserIdAndDeletedAtIsNull(USER_ID)).thenReturn(List.of(avulso));

        List<TripSummaryDTO> trips = service.findAllTrips(USER_ID);

        assertEquals(1, trips.size());
        assertNull(trips.get(0).tripGroupId());
        assertEquals(1, trips.get(0).legs().size());
        assertEquals("Lisboa", trips.get(0).title());
    }

    @Test
    void findAllTrips_shouldKeepSeparateStandaloneItineraries_asSeparateGroups() {
        Destination lisboa = destinationWith("Lisboa");
        Destination porto = destinationWith("Porto");
        Itinerary itLisboa = itineraryWith(UUID.randomUUID(), lisboa,
            LocalDate.of(2027, 10, 1), LocalDate.of(2027, 10, 5), "Lisboa");
        Itinerary itPorto = itineraryWith(UUID.randomUUID(), porto,
            LocalDate.of(2027, 11, 1), LocalDate.of(2027, 11, 3), "Porto");

        when(itineraryRepo.findAllByUserIdAndDeletedAtIsNull(USER_ID)).thenReturn(List.of(itLisboa, itPorto));

        List<TripSummaryDTO> trips = service.findAllTrips(USER_ID);

        assertEquals(2, trips.size());
    }

    // ─── findTripByGroupId ──────────────────────────────────────────────────

    @Test
    void findTripByGroupId_shouldReturnLegsInChronologicalOrder() {
        UUID tripGroupId = UUID.randomUUID();
        Destination paris = destinationWith("Paris");
        Destination amsterdam = destinationWith("Amsterdã");

        Itinerary legParis = itineraryWith(UUID.randomUUID(), paris,
            LocalDate.of(2027, 9, 1), LocalDate.of(2027, 9, 4), "Paris");
        legParis.setTripGroupId(tripGroupId);
        legParis.setTripGroupOrder(0);

        Itinerary legAmsterdam = itineraryWith(UUID.randomUUID(), amsterdam,
            LocalDate.of(2027, 9, 5), LocalDate.of(2027, 9, 7), "Amsterdã");
        legAmsterdam.setTripGroupId(tripGroupId);
        legAmsterdam.setTripGroupOrder(1);

        when(itineraryRepo.findAllByTripGroupIdAndUserIdAndDeletedAtIsNullOrderByTripGroupOrderAsc(tripGroupId, USER_ID))
            .thenReturn(List.of(legParis, legAmsterdam));

        TripSummaryDTO trip = service.findTripByGroupId(tripGroupId, USER_ID);

        assertEquals(tripGroupId, trip.tripGroupId());
        assertEquals(2, trip.legs().size());
    }

    @Test
    void findTripByGroupId_shouldThrowResourceNotFound_whenNoLegsExist() {
        UUID tripGroupId = UUID.randomUUID();
        when(itineraryRepo.findAllByTripGroupIdAndUserIdAndDeletedAtIsNullOrderByTripGroupOrderAsc(tripGroupId, USER_ID))
            .thenReturn(List.of());

        assertThrows(ResourceNotFoundException.class, () -> service.findTripByGroupId(tripGroupId, USER_ID));
    }

    // ─── Limite mensal do plano gratuito (1 roteiro/mês, ver luvoy-pendencias.md) ─

    @Test
    void generate_shouldSucceed_whenFreeUserHasNoItineraryThisMonth() {
        ItineraryRequest request = requestFor(UUID.randomUUID(), LocalDate.of(2027, 9, 1), LocalDate.of(2027, 9, 4));
        when(engineService.generate(USER_ID, request))
            .thenReturn(new ItineraryResponseDTO(UUID.randomUUID(), "Roteiro", request.destinationId(), "Destino", "", "Teste", request.startDate(), request.endDate(), List.of()));

        assertDoesNotThrow(() -> service.generate(USER_ID, request));
        verify(engineService).generate(USER_ID, request);
    }

    @Test
    void generate_shouldThrowPaymentRequired_whenFreeUserAlreadyGeneratedOneThisMonth() {
        Destination paris = destinationWith("Paris");
        Itinerary generatedThisMonth = itineraryWith(UUID.randomUUID(), paris,
            LocalDate.of(2027, 9, 1), LocalDate.of(2027, 9, 4), "Paris");
        generatedThisMonth.setCreatedAt(LocalDateTime.now());

        when(itineraryRepo.findAllByUserIdAndDeletedAtIsNull(USER_ID)).thenReturn(List.of(generatedThisMonth));

        ItineraryRequest request = requestFor(UUID.randomUUID(), LocalDate.of(2027, 10, 1), LocalDate.of(2027, 10, 4));
        assertThrows(PaymentRequiredException.class, () -> service.generate(USER_ID, request));
        verifyNoInteractions(engineService);
    }

    @Test
    void generate_shouldSucceed_whenFreeUsersOnlyItineraryThisMonthWasCreatedInAPreviousMonth() {
        Destination paris = destinationWith("Paris");
        Itinerary generatedLastMonth = itineraryWith(UUID.randomUUID(), paris,
            LocalDate.of(2027, 8, 1), LocalDate.of(2027, 8, 4), "Paris");
        generatedLastMonth.setCreatedAt(LocalDateTime.now().minusMonths(1));

        when(itineraryRepo.findAllByUserIdAndDeletedAtIsNull(USER_ID)).thenReturn(List.of(generatedLastMonth));

        ItineraryRequest request = requestFor(UUID.randomUUID(), LocalDate.of(2027, 9, 1), LocalDate.of(2027, 9, 4));
        when(engineService.generate(USER_ID, request))
            .thenReturn(new ItineraryResponseDTO(UUID.randomUUID(), "Roteiro", request.destinationId(), "Destino", "", "Teste", request.startDate(), request.endDate(), List.of()));

        assertDoesNotThrow(() -> service.generate(USER_ID, request));
    }

    @Test
    void generate_shouldNeverThrow_forPremiumUser_regardlessOfHowManyItinerariesThisMonth() {
        when(userRepo.findById(USER_ID)).thenReturn(Optional.of(premiumUser()));

        Destination paris = destinationWith("Paris");
        Itinerary already = itineraryWith(UUID.randomUUID(), paris,
            LocalDate.of(2027, 9, 1), LocalDate.of(2027, 9, 4), "Paris");
        already.setCreatedAt(LocalDateTime.now());
        when(itineraryRepo.findAllByUserIdAndDeletedAtIsNull(USER_ID)).thenReturn(List.of(already));

        ItineraryRequest request = requestFor(UUID.randomUUID(), LocalDate.of(2027, 9, 10), LocalDate.of(2027, 9, 12));
        when(engineService.generate(USER_ID, request))
            .thenReturn(new ItineraryResponseDTO(UUID.randomUUID(), "Roteiro", request.destinationId(), "Destino", "", "Teste", request.startDate(), request.endDate(), List.of()));

        assertDoesNotThrow(() -> service.generate(USER_ID, request));
    }

    @Test
    void generateTrip_shouldThrowPaymentRequired_whenFreeUserAlreadyGeneratedOneThisMonth() {
        Destination paris = destinationWith("Paris");
        Itinerary generatedThisMonth = itineraryWith(UUID.randomUUID(), paris,
            LocalDate.of(2027, 9, 1), LocalDate.of(2027, 9, 4), "Paris");
        generatedThisMonth.setCreatedAt(LocalDateTime.now());
        when(itineraryRepo.findAllByUserIdAndDeletedAtIsNull(USER_ID)).thenReturn(List.of(generatedThisMonth));

        ItineraryRequest legRoma = requestFor(UUID.randomUUID(), LocalDate.of(2027, 10, 1), LocalDate.of(2027, 10, 4));
        assertThrows(PaymentRequiredException.class,
            () -> service.generateTrip(USER_ID, new CreateTripRequest(List.of(legRoma))));
        verifyNoInteractions(engineService);
    }

    @Test
    void generateTrip_withMultipleLegs_shouldCountAsOnlyOneUnit_againstTheMonthlyLimit() {
        // Uma viagem multi-destino de 2 trechos, gerada de uma vez, não pode
        // "gastar" 2 unidades do limite mensal — é 1 ação de criação só.
        Destination paris = destinationWith("Paris");
        Destination amsterdam = destinationWith("Amsterdã");

        UUID legIdParis = UUID.randomUUID();
        UUID legIdAmsterdam = UUID.randomUUID();
        ItineraryRequest legParis = requestFor(paris.getId(), LocalDate.of(2027, 9, 1), LocalDate.of(2027, 9, 4));
        ItineraryRequest legAmsterdam = requestFor(amsterdam.getId(), LocalDate.of(2027, 9, 5), LocalDate.of(2027, 9, 7));

        Itinerary itineraryParis = itineraryWith(legIdParis, paris, legParis.startDate(), legParis.endDate(), "Paris");
        Itinerary itineraryAmsterdam = itineraryWith(legIdAmsterdam, amsterdam, legAmsterdam.startDate(), legAmsterdam.endDate(), "Amsterdã");

        when(engineService.generate(eq(USER_ID), eq(legParis), any(TripSaturationLedger.class)))
            .thenReturn(new ItineraryResponseDTO(legIdParis, "Paris", paris.getId(), "Paris", "", "Teste", legParis.startDate(), legParis.endDate(), List.of()));
        when(engineService.generate(eq(USER_ID), eq(legAmsterdam), any(TripSaturationLedger.class)))
            .thenReturn(new ItineraryResponseDTO(legIdAmsterdam, "Amsterdã", amsterdam.getId(), "Amsterdã", "", "Teste", legAmsterdam.startDate(), legAmsterdam.endDate(), List.of()));
        when(itineraryRepo.findById(legIdParis)).thenReturn(Optional.of(itineraryParis));
        when(itineraryRepo.findById(legIdAmsterdam)).thenReturn(Optional.of(itineraryAmsterdam));
        when(itineraryRepo.save(any(Itinerary.class))).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() ->
            service.generateTrip(USER_ID, new CreateTripRequest(List.of(legParis, legAmsterdam))));
    }

    // ─── Ledger de saturação compartilhado entre trechos ───────────────────

    @Test
    void generateTrip_shouldShareTheSameSaturationLedger_acrossAllLegs() {
        // Contrato: o ledger de variedade de categoria/mustSee (ver
        // TripSaturationLedger) deve ser UM só, criado antes do loop de
        // trechos, e reaproveitado pra todos — nunca um ledger novo por
        // trecho. É isso que faz o motor "lembrar" que já visitou muitos
        // museus em Paris quando for escolher POIs em Roma.
        Destination paris = destinationWith("Paris");
        Destination roma = destinationWith("Roma");

        UUID legIdParis = UUID.randomUUID();
        UUID legIdRoma = UUID.randomUUID();
        ItineraryRequest legParis = requestFor(paris.getId(), LocalDate.of(2027, 9, 1), LocalDate.of(2027, 9, 4));
        ItineraryRequest legRoma = requestFor(roma.getId(), LocalDate.of(2027, 9, 5), LocalDate.of(2027, 9, 7));

        Itinerary itineraryParis = itineraryWith(legIdParis, paris, legParis.startDate(), legParis.endDate(), "Paris");
        Itinerary itineraryRoma = itineraryWith(legIdRoma, roma, legRoma.startDate(), legRoma.endDate(), "Roma");

        when(engineService.generate(eq(USER_ID), eq(legParis), any(TripSaturationLedger.class)))
            .thenReturn(new ItineraryResponseDTO(legIdParis, "Paris", paris.getId(), "Paris", "", "Teste", legParis.startDate(), legParis.endDate(), List.of()));
        when(engineService.generate(eq(USER_ID), eq(legRoma), any(TripSaturationLedger.class)))
            .thenReturn(new ItineraryResponseDTO(legIdRoma, "Roma", roma.getId(), "Roma", "", "Teste", legRoma.startDate(), legRoma.endDate(), List.of()));
        when(itineraryRepo.findById(legIdParis)).thenReturn(Optional.of(itineraryParis));
        when(itineraryRepo.findById(legIdRoma)).thenReturn(Optional.of(itineraryRoma));
        when(itineraryRepo.save(any(Itinerary.class))).thenAnswer(inv -> inv.getArgument(0));

        service.generateTrip(USER_ID, new CreateTripRequest(List.of(legParis, legRoma)));

        org.mockito.ArgumentCaptor<TripSaturationLedger> ledgerCaptor =
            org.mockito.ArgumentCaptor.forClass(TripSaturationLedger.class);
        verify(engineService, times(2)).generate(eq(USER_ID), any(ItineraryRequest.class), ledgerCaptor.capture());

        List<TripSaturationLedger> capturedLedgers = ledgerCaptor.getAllValues();
        assertSame(capturedLedgers.get(0), capturedLedgers.get(1),
            "O mesmo ledger deve ser passado para todos os trechos da viagem, não um novo por trecho");
    }
}
