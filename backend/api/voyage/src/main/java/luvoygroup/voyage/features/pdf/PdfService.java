package luvoygroup.voyage.features.pdf;

import luvoygroup.voyage.domain.Itinerary;
import luvoygroup.voyage.exception.ForbiddenAccessException;
import luvoygroup.voyage.exception.ResourceNotFoundException;
import luvoygroup.voyage.repository.ItineraryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PdfService {

    private final ItineraryRepository itineraryRepository;

    public Map<String, String> export(UUID userId, UUID itineraryId) {
        Itinerary itinerary = itineraryRepository.findByIdAndDeletedAtIsNull(itineraryId)
            .orElseThrow(ResourceNotFoundException::new);

        if (!itinerary.getUser().getId().equals(userId)) {
            throw new ForbiddenAccessException();
        }

        // PDF generation via Flutter client-side — backend apenas valida acesso e retorna URL placeholder
        String pdfUrl = "https://storage.luvoy.com/itineraries/" + itineraryId + ".pdf";
        return Map.of("pdfUrl", pdfUrl);
    }
}
