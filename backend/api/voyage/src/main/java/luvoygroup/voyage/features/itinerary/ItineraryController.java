package luvoygroup.voyage.features.itinerary;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import luvoygroup.voyage.features.itinerary.dto.*;
import luvoygroup.voyage.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/itineraries")
@RequiredArgsConstructor
@Tag(name = "Itineraries", description = "Geração e gerenciamento de roteiros personalizados")
public class ItineraryController {

    private final ItineraryService itineraryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Gera um novo roteiro personalizado")
    public ItineraryResponseDTO generate(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @Valid @RequestBody ItineraryRequest request
    ) {
        return itineraryService.generate(UUID.fromString(principal.id()), request);
    }

    @GetMapping
    @Operation(summary = "Lista todos os roteiros do usuário autenticado")
    public List<ItinerarySummaryDTO> listAll(@AuthenticationPrincipal AuthenticatedUser principal) {
        return itineraryService.findAll(UUID.fromString(principal.id()));
    }

    @PostMapping("/trips")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Gera uma viagem multi-destino: N trechos (cada um um roteiro completo), " +
        "agrupados sob a mesma viagem. Trechos não podem se sobrepor em data.")
    public TripSummaryDTO generateTrip(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @Valid @RequestBody CreateTripRequest request
    ) {
        return itineraryService.generateTrip(UUID.fromString(principal.id()), request);
    }

    @GetMapping("/trips")
    @Operation(summary = "Lista todas as viagens do usuário autenticado, agrupadas — " +
        "um roteiro avulso vira um grupo de 1 trecho só")
    public List<TripSummaryDTO> listAllTrips(@AuthenticationPrincipal AuthenticatedUser principal) {
        return itineraryService.findAllTrips(UUID.fromString(principal.id()));
    }

    @GetMapping("/trips/{tripGroupId}")
    @Operation(summary = "Retorna os trechos de uma viagem multi-destino, na ordem cronológica")
    public TripSummaryDTO getTripByGroupId(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @PathVariable UUID tripGroupId
    ) {
        return itineraryService.findTripByGroupId(tripGroupId, UUID.fromString(principal.id()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Retorna um roteiro completo com todos os POIs e dias")
    public ItineraryResponseDTO getById(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @PathVariable UUID id
    ) {
        return itineraryService.findById(id, UUID.fromString(principal.id()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove um roteiro (soft delete)")
    public void delete(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @PathVariable UUID id
    ) {
        itineraryService.delete(id, UUID.fromString(principal.id()));
    }

    @GetMapping("/{id}/activities/{activityId}/alternatives")
    @Operation(summary = "Lista até 3 POIs alternativos para substituir uma atividade")
    public List<PoiDetailDTO> getAlternatives(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @PathVariable UUID id,
        @PathVariable UUID activityId
    ) {
        return itineraryService.getAlternatives(id, activityId, UUID.fromString(principal.id()));
    }

    @PatchMapping("/{id}/activities/{activityId}")
    @Operation(summary = "Substitui o POI de uma atividade no roteiro")
    public ActivityDTO substituteActivity(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @PathVariable UUID id,
        @PathVariable UUID activityId,
        @Valid @RequestBody SubstituteRequest request
    ) {
        return itineraryService.substituteActivity(id, activityId, request.poiId(), UUID.fromString(principal.id()));
    }
}
