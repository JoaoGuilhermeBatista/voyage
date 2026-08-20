package luvoygroup.voyage.features.itinerarytemplates;

import luvoygroup.voyage.domain.enums.BudgetTier;
import luvoygroup.voyage.features.itinerary.dto.ItineraryResponseDTO;
import luvoygroup.voyage.features.itinerarytemplates.dto.ItineraryTemplateDTO;
import luvoygroup.voyage.features.itinerarytemplates.dto.ItineraryTemplateDetailDTO;
import luvoygroup.voyage.features.itinerarytemplates.dto.ItineraryTemplateSummaryDTO;
import luvoygroup.voyage.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * STUB — todos os métodos abaixo lançam UnsupportedOperationException.
 *
 * Mesmo padrão TDD-red estabelecido para a feature Maps (ver
 * MapsService.java / MapsServiceTest.java): um agente seguinte implementa a
 * lógica real depois que os testes abaixo (ItineraryTemplateServiceTest)
 * forem aprovados. Contrato completo de cada método — o que carregar, o
 * que validar, o que persistir e o shape de retorno — está documentado nos
 * testes, não aqui.
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
        throw new UnsupportedOperationException("ItineraryTemplateService.generateForDestination ainda não implementado");
    }

    public List<ItineraryTemplateSummaryDTO> findAllForDestination(String destinationSlug) {
        throw new UnsupportedOperationException("ItineraryTemplateService.findAllForDestination ainda não implementado");
    }

    public ItineraryTemplateDetailDTO findById(UUID templateId) {
        throw new UnsupportedOperationException("ItineraryTemplateService.findById ainda não implementado");
    }

    public ItineraryResponseDTO adopt(UUID templateId, UUID userId, LocalDate startDate) {
        throw new UnsupportedOperationException("ItineraryTemplateService.adopt ainda não implementado");
    }
}
