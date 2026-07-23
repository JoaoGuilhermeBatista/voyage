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
}
