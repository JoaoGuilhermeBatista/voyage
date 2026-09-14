package luvoygroup.voyage.features.itinerary;

import luvoygroup.voyage.domain.Itinerary;
import luvoygroup.voyage.domain.User;
import luvoygroup.voyage.exception.ForbiddenAccessException;
import luvoygroup.voyage.exception.InvalidDateRangeException;
import luvoygroup.voyage.exception.PaymentRequiredException;
import luvoygroup.voyage.exception.ResourceNotFoundException;
import luvoygroup.voyage.features.itinerary.dto.*;
import luvoygroup.voyage.repository.ItineraryActivityRepository;
import luvoygroup.voyage.repository.ItineraryRepository;
import luvoygroup.voyage.repository.PoiRepository;
import luvoygroup.voyage.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ItineraryService {

    // Plano gratuito: 1 roteiro (ou viagem multi-destino inteira, que conta
    // como 1 só) por mês corrido — decisão do usuário em 2026-09-08, ver
    // luvoy-pendencias.md. Assinante premium (User.isPremium) não passa por
    // essa checagem, sempre ilimitado.
    private static final int FREE_MONTHLY_ITINERARY_LIMIT = 1;

    private final ItineraryEngineService engineService;
    private final ItineraryRepository itineraryRepo;
    private final ItineraryActivityRepository activityRepo;
    private final PoiRepository poiRepo;
    private final UserRepository userRepo;

    public ItineraryResponseDTO generate(UUID userId, ItineraryRequest request) {
        enforceMonthlyLimit(userId);
        return engineService.generate(userId, request);
    }

    @Transactional(readOnly = true)
    public List<ItinerarySummaryDTO> findAll(UUID userId) {
        return itineraryRepo.findAllByUserIdAndDeletedAtIsNull(userId).stream()
            .map(ItinerarySummaryDTO::from)
            .toList();
    }

    /**
     * Mesma lista de {@link #findAll}, mas agrupada por viagem (spec: "Um
     * card agrupado" pra multi-destino) — um roteiro avulso (tripGroupId
     * nulo) vira um grupo de 1 elemento só, mesmo shape de uma viagem
     * multi-destino de verdade, pro front-end tratar os dois casos de forma
     * uniforme (grupo com legs.size()==1 = sem UI de trechos).
     */
    @Transactional(readOnly = true)
    public List<TripSummaryDTO> findAllTrips(UUID userId) {
        List<Itinerary> all = itineraryRepo.findAllByUserIdAndDeletedAtIsNull(userId);

        Map<UUID, List<Itinerary>> grouped = new LinkedHashMap<>();
        for (Itinerary it : all) {
            UUID groupKey = it.getTripGroupId() != null ? it.getTripGroupId() : it.getId();
            grouped.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(it);
        }

        return grouped.values().stream()
            .map(legs -> {
                List<Itinerary> sorted = legs.stream()
                    .sorted((a, b) -> {
                        Integer orderA = a.getTripGroupOrder();
                        Integer orderB = b.getTripGroupOrder();
                        if (orderA == null || orderB == null) return 0;
                        return orderA.compareTo(orderB);
                    })
                    .toList();
                return TripSummaryDTO.from(sorted);
            })
            .toList();
    }

    /**
     * Gera uma viagem multi-destino: um {@link Itinerary} normal por
     * trecho (via {@link ItineraryEngineService#generate}, reaproveitado
     * 100% — nenhuma mudança no motor de geração), todos marcados com o
     * mesmo {@code tripGroupId} novo e {@code tripGroupOrder} = posição na
     * lista de trechos. Valida que os trechos não se sobrepõem em data
     * ANTES de gerar qualquer coisa (spec: "Permitir folga, proibir
     * sobreposição") — a ordem da lista é a ordem cronológica pretendida.
     */
    @Transactional
    public TripSummaryDTO generateTrip(UUID userId, CreateTripRequest request) {
        enforceMonthlyLimit(userId);
        List<ItineraryRequest> legs = request.legs();
        validateNoOverlap(legs);

        UUID tripGroupId = UUID.randomUUID();
        List<Itinerary> createdLegs = new ArrayList<>();

        for (int i = 0; i < legs.size(); i++) {
            ItineraryResponseDTO generated = engineService.generate(userId, legs.get(i));
            Itinerary itinerary = itineraryRepo.findById(generated.id())
                .orElseThrow(() -> new ResourceNotFoundException("Roteiro recém-gerado não encontrado"));
            itinerary.setTripGroupId(tripGroupId);
            itinerary.setTripGroupOrder(i);
            createdLegs.add(itineraryRepo.save(itinerary));
        }

        return TripSummaryDTO.from(createdLegs);
    }

    /**
     * Busca os trechos de uma viagem multi-destino, na ordem cronológica
     * (tripGroupOrder), pra abrir a partir do card agrupado.
     */
    @Transactional(readOnly = true)
    public TripSummaryDTO findTripByGroupId(UUID tripGroupId, UUID userId) {
        List<Itinerary> legs = itineraryRepo
            .findAllByTripGroupIdAndUserIdAndDeletedAtIsNullOrderByTripGroupOrderAsc(tripGroupId, userId);

        if (legs.isEmpty())
            throw new ResourceNotFoundException("Viagem não encontrada");

        return TripSummaryDTO.from(legs);
    }

    /**
     * Plano gratuito: 1 roteiro (avulso OU viagem multi-destino inteira,
     * que conta como 1 unidade só) por mês corrido. Assinante premium pula
     * a checagem inteira. "Mês corrido" = desde o dia 1 às 00:00 no fuso do
     * servidor até agora — reseta sozinho a cada mês, sem job/cron.
     */
    private void enforceMonthlyLimit(UUID userId) {
        User user = userRepo.findById(userId).orElseThrow(ResourceNotFoundException::new);
        if (user.hasActivePremium()) return;

        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        long tripsThisMonth = itineraryRepo.findAllByUserIdAndDeletedAtIsNull(userId).stream()
            .filter(it -> it.getCreatedAt() != null && !it.getCreatedAt().isBefore(startOfMonth))
            .map(it -> it.getTripGroupId() != null ? it.getTripGroupId() : it.getId())
            .distinct()
            .count();

        if (tripsThisMonth >= FREE_MONTHLY_ITINERARY_LIMIT) {
            throw new PaymentRequiredException();
        }
    }

    private void validateNoOverlap(List<ItineraryRequest> legs) {
        for (int i = 0; i < legs.size() - 1; i++) {
            ItineraryRequest current = legs.get(i);
            ItineraryRequest next = legs.get(i + 1);

            if (!next.startDate().isAfter(current.endDate()))
                throw new InvalidDateRangeException(
                    "Os trechos da viagem não podem se sobrepor: o trecho %d termina em %s e o trecho %d começa em %s"
                        .formatted(i + 1, current.endDate(), i + 2, next.startDate())
                );
        }
    }

    @Transactional(readOnly = true)
    public ItineraryResponseDTO findById(UUID id, UUID userId) {
        var itinerary = itineraryRepo.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new ResourceNotFoundException("Roteiro não encontrado"));

        if (!itinerary.getUser().getId().equals(userId))
            throw new ForbiddenAccessException("Acesso não autorizado a este roteiro");

        List<DayDTO> days = itinerary.getDays().stream()
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
            itinerary.getDestination().getId(),
            itinerary.getDestination().getName(),
            itinerary.getDestination().getHeroImageUrl(),
            itinerary.getStartDate(),
            itinerary.getEndDate(),
            days
        );
    }

    @Transactional(readOnly = true)
    public List<PoiDetailDTO> getAlternatives(UUID itineraryId, UUID activityId, UUID userId) {
        var itinerary = itineraryRepo.findByIdAndDeletedAtIsNull(itineraryId)
            .orElseThrow(() -> new ResourceNotFoundException("Roteiro não encontrado"));

        if (!itinerary.getUser().getId().equals(userId))
            throw new ForbiddenAccessException();

        var activity = activityRepo.findByIdAndItineraryDayItineraryId(activityId, itineraryId)
            .orElseThrow(() -> new ResourceNotFoundException("Atividade não encontrada"));

        List<UUID> usedIds = activityRepo.findAllPoiIdsByItineraryId(itineraryId);
        UUID destinationId = itinerary.getDestination().getId();

        return poiRepo.findAlternatives(destinationId, activity.getPoi().getCategory(), usedIds)
            .stream()
            .limit(3)
            .map(PoiDetailDTO::from)
            .toList();
    }

    @Transactional
    public ActivityDTO substituteActivity(UUID itineraryId, UUID activityId, UUID poiId, UUID userId) {
        var itinerary = itineraryRepo.findByIdAndDeletedAtIsNull(itineraryId)
            .orElseThrow(() -> new ResourceNotFoundException("Roteiro não encontrado"));

        if (!itinerary.getUser().getId().equals(userId))
            throw new ForbiddenAccessException();

        var activity = activityRepo.findByIdAndItineraryDayItineraryId(activityId, itineraryId)
            .orElseThrow(() -> new ResourceNotFoundException("Atividade não encontrada"));

        var newPoi = poiRepo.findById(poiId)
            .orElseThrow(() -> new ResourceNotFoundException("POI não encontrado"));

        if (!newPoi.getDestination().getId().equals(itinerary.getDestination().getId()))
            throw new ForbiddenAccessException("O POI não pertence ao destino deste roteiro");

        activity.setPoi(newPoi);
        activityRepo.save(activity);

        return new ActivityDTO(activity.getId(), activity.getSlot(), activity.getOrderInSlot(), PoiDetailDTO.from(newPoi));
    }

    @Transactional
    public void delete(UUID id, UUID userId) {
        var itinerary = itineraryRepo.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new ResourceNotFoundException("Roteiro não encontrado"));

        if (!itinerary.getUser().getId().equals(userId))
            throw new ForbiddenAccessException("Acesso não autorizado a este roteiro");

        itinerary.setDeletedAt(LocalDateTime.now());
        itineraryRepo.save(itinerary);
    }
}
