package luvoygroup.voyage.features.itinerary;

import luvoygroup.voyage.exception.ForbiddenAccessException;
import luvoygroup.voyage.exception.ResourceNotFoundException;
import luvoygroup.voyage.features.itinerary.dto.*;
import luvoygroup.voyage.repository.ItineraryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ItineraryService {

    private final ItineraryEngineService engineService;
    private final ItineraryRepository itineraryRepo;

    public ItineraryResponseDTO generate(UUID userId, ItineraryRequest request) {
        return engineService.generate(userId, request);
    }

    @Transactional(readOnly = true)
    public List<ItinerarySummaryDTO> findAll(UUID userId) {
        return itineraryRepo.findAllByUserIdAndDeletedAtIsNull(userId).stream()
            .map(ItinerarySummaryDTO::from)
            .toList();
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
