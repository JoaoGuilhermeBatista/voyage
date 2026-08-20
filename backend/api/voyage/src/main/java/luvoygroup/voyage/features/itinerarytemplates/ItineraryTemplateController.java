package luvoygroup.voyage.features.itinerarytemplates;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import luvoygroup.voyage.features.itinerary.dto.ItineraryResponseDTO;
import luvoygroup.voyage.features.itinerarytemplates.dto.AdoptTemplateRequest;
import luvoygroup.voyage.features.itinerarytemplates.dto.ItineraryTemplateDetailDTO;
import luvoygroup.voyage.features.itinerarytemplates.dto.ItineraryTemplateSummaryDTO;
import luvoygroup.voyage.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Endpoints públicos de templates de roteiro curados por destino, e adoção
 * de um template em um roteiro real do usuário autenticado.
 *
 * As duas rotas GET ficam sob /api/v1/destinations/{slug}/... e por isso já
 * herdam permitAll() do SecurityConfig (que libera GET /api/v1/destinations/**),
 * sem precisar mexer no SecurityConfig. A rota POST /adopt não bate nesse
 * padrão e cai no anyRequest().authenticated() default — exige JWT.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Itinerary Templates", description = "Templates de roteiro curados por destino e adoção em um roteiro real")
public class ItineraryTemplateController {

    private final ItineraryTemplateService itineraryTemplateService;

    @GetMapping("/api/v1/destinations/{slug}/itinerary-templates")
    @Operation(summary = "Lista os templates de roteiro curados de um destino")
    public List<ItineraryTemplateSummaryDTO> listForDestination(@PathVariable String slug) {
        return itineraryTemplateService.findAllForDestination(slug);
    }

    @GetMapping("/api/v1/destinations/{slug}/itinerary-templates/{templateId}")
    @Operation(summary = "Retorna o preview completo de um template (sem datas)")
    public ItineraryTemplateDetailDTO getById(
        @PathVariable String slug,
        @PathVariable UUID templateId
    ) {
        return itineraryTemplateService.findById(templateId);
    }

    @PostMapping("/api/v1/itinerary-templates/{templateId}/adopt")
    @Operation(summary = "Adota um template como um roteiro real, datado e editável, do usuário autenticado")
    public ItineraryResponseDTO adopt(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @PathVariable UUID templateId,
        @Valid @RequestBody AdoptTemplateRequest request
    ) {
        return itineraryTemplateService.adopt(templateId, UUID.fromString(principal.id()), request.startDate());
    }
}
