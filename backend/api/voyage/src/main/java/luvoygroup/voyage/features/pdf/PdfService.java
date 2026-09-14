package luvoygroup.voyage.features.pdf;

import luvoygroup.voyage.domain.Itinerary;
import luvoygroup.voyage.domain.User;
import luvoygroup.voyage.exception.ForbiddenAccessException;
import luvoygroup.voyage.exception.PaymentRequiredException;
import luvoygroup.voyage.exception.ResourceNotFoundException;
import luvoygroup.voyage.repository.ItineraryRepository;
import luvoygroup.voyage.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PdfService {

    private final ItineraryRepository itineraryRepository;
    private final UserRepository userRepository;

    @Transactional
    public Map<String, String> export(UUID userId, UUID itineraryId) {
        Itinerary itinerary = itineraryRepository.findByIdAndDeletedAtIsNull(itineraryId)
            .orElseThrow(ResourceNotFoundException::new);

        if (!itinerary.getUser().getId().equals(userId)) {
            throw new ForbiddenAccessException();
        }

        User user = userRepository.findById(userId)
            .orElseThrow(ResourceNotFoundException::new);

        if (!user.hasActivePremium()) {
            if (user.getPdfExportCount() < 1) {
                user.setPdfExportCount(user.getPdfExportCount() + 1);
            } else if (user.getExtraPdfCredits() > 0) {
                // 1ª exportação grátis já usada — consome 1 crédito avulso
                // comprado via Pix em vez de bloquear direto.
                user.setExtraPdfCredits(user.getExtraPdfCredits() - 1);
            } else {
                throw new PaymentRequiredException();
            }
            userRepository.save(user);
        }

        // PDF gerado client-side no Flutter — backend valida acesso e retorna referência
        String pdfUrl = "itineraries/" + itineraryId + ".pdf";
        return Map.of("pdfUrl", pdfUrl);
    }
}
