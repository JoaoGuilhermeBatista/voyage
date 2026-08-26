package luvoygroup.voyage.features.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import luvoygroup.voyage.features.admin.dto.BatchGenerationReport;
import luvoygroup.voyage.features.admin.dto.GenerateDestinationRequest;
import luvoygroup.voyage.features.admin.dto.PoiEnrichmentReport;
import luvoygroup.voyage.features.destinations.dto.DestinationSummaryDTO;
import luvoygroup.voyage.features.itinerarytemplates.dto.GenerateTemplateRequest;
import luvoygroup.voyage.features.itinerarytemplates.dto.ItineraryTemplateDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Endpoints administrativos (requer X-Admin-Token)")
public class AdminController {

    private final AdminService adminService;

    @Value("${admin.token:not-configured}")
    private String adminToken;

    @PostMapping("/destinations/generate")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Gera e salva um novo destino usando GPT-4o")
    public ResponseEntity<DestinationSummaryDTO> generateDestination(
        @RequestHeader("X-Admin-Token") String token,
        @Valid @RequestBody GenerateDestinationRequest request
    ) {
        if (!adminToken.equals(token))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(adminService.generateAndSaveDestination(request));
    }

    @PostMapping("/pois/generate-batch")
    @Operation(summary = "Gera POIs via GPT-4o para todos os destinos sem POIs, de 5 em 5. Para no primeiro erro.")
    public ResponseEntity<BatchGenerationReport> generatePoisBatch(
        @RequestHeader("X-Admin-Token") String token
    ) {
        if (!adminToken.equals(token))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        return ResponseEntity.ok(adminService.generatePoisBatch());
    }

    @PostMapping("/destinations/{slug}/itinerary-templates/generate")
    @Operation(summary = "Gera um novo template de roteiro para o destino via GPT-4o, escolhendo apenas entre POIs reais já cadastrados")
    public ResponseEntity<ItineraryTemplateDTO> generateItineraryTemplate(
        @RequestHeader(value = "X-Admin-Token", required = false) String token,
        @PathVariable String slug,
        @Valid @RequestBody GenerateTemplateRequest request
    ) {
        if (token == null || !adminToken.equals(token))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(adminService.generateItineraryTemplate(slug, request));
    }

    @PostMapping("/pois/enrich-catholic-church-flags")
    @Operation(summary = "Classifica retroativamente, via GPT-4o, quais POIs já cadastrados são igrejas " +
        "católicas, atualizando catholicChurch IN PLACE (nunca cria ou deleta POIs)")
    public ResponseEntity<PoiEnrichmentReport> enrichCatholicChurchFlags(
        @RequestHeader(value = "X-Admin-Token", required = false) String token
    ) {
        if (token == null || !adminToken.equals(token))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        return ResponseEntity.ok(adminService.enrichExistingPoisWithCatholicChurchFlag());
    }
}
