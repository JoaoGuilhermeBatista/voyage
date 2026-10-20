package luvoygroup.voyage.features.maps;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import luvoygroup.voyage.domain.enums.BudgetTier;
import luvoygroup.voyage.features.maps.dto.DirectionsResponseDTO;
import luvoygroup.voyage.features.maps.dto.EmbedUrlResponseDTO;
import luvoygroup.voyage.features.maps.dto.NeighborhoodSuggestionsDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Proxy autenticado para o Google Maps Platform (Directions + Embed).
 *
 * Diferente do endpoint de câmbio (dados públicos e gratuitos), este
 * proxy expõe chamadas a uma API paga do Google — por isso NÃO está na
 * lista de permitAll() do SecurityConfig; exige JWT válido como qualquer
 * outro endpoint autenticado (401 sem token).
 *
 * A validação de parâmetros abaixo é real (não faz parte da lógica de
 * negócio "stub"): garante que os 4/2 parâmetros obrigatórios existam e
 * sejam números válidos antes de repassar para {@link MapsService}, que
 * por sua vez ainda é um esqueleto (ver MapsService.java).
 */
@RestController
@RequestMapping("/api/v1/maps")
@RequiredArgsConstructor
@Tag(name = "Maps", description = "Proxy autenticado para Google Directions API e Google Maps Embed API")
public class MapsController {

    private final MapsService mapsService;
    private final PlacesService placesService;

    @GetMapping("/directions")
    @Operation(summary = "Rota entre dois pontos via Google Directions API")
    public DirectionsResponseDTO getDirections(
        @RequestParam(required = false) String originLat,
        @RequestParam(required = false) String originLng,
        @RequestParam(required = false) String destLat,
        @RequestParam(required = false) String destLng
    ) {
        double oLat = parseCoord(originLat, "originLat");
        double oLng = parseCoord(originLng, "originLng");
        double dLat = parseCoord(destLat, "destLat");
        double dLng = parseCoord(destLng, "destLng");
        return mapsService.getDirections(oLat, oLng, dLat, dLng);
    }

    @GetMapping("/directions-between-destinations")
    @Operation(summary = "Rota entre as CIDADES de dois destinos (ex.: trechos de uma viagem multi-destino)")
    public DirectionsResponseDTO getDirectionsBetweenDestinations(
        @RequestParam UUID originDestinationId,
        @RequestParam UUID destDestinationId
    ) {
        return mapsService.getDirectionsBetweenDestinations(originDestinationId, destDestinationId);
    }

    @GetMapping("/embed-url")
    @Operation(summary = "URL embutível do Google Maps para um ponto (lat/lng)")
    public EmbedUrlResponseDTO getEmbedUrl(
        @RequestParam(required = false) String lat,
        @RequestParam(required = false) String lng
    ) {
        double parsedLat = parseCoord(lat, "lat");
        double parsedLng = parseCoord(lng, "lng");
        return mapsService.getEmbedUrl(parsedLat, parsedLng);
    }

    @GetMapping("/embed-url/directions")
    @Operation(summary = "URL embutível do Google Maps para a rota entre dois pontos (origem/destino)")
    public EmbedUrlResponseDTO getDirectionsEmbedUrl(
        @RequestParam(required = false) String originLat,
        @RequestParam(required = false) String originLng,
        @RequestParam(required = false) String destLat,
        @RequestParam(required = false) String destLng
    ) {
        double oLat = parseCoord(originLat, "originLat");
        double oLng = parseCoord(originLng, "originLng");
        double dLat = parseCoord(destLat, "destLat");
        double dLng = parseCoord(destLng, "destLng");
        return mapsService.getDirectionsEmbedUrl(oLat, oLng, dLat, dLng);
    }

    @GetMapping("/neighborhood-suggestions")
    @Operation(summary = "Sugestão de bairros para hospedagem via Google Places Nearby Search")
    public NeighborhoodSuggestionsDTO getNeighborhoodSuggestions(
        @RequestParam UUID destinationId,
        @RequestParam BudgetTier budgetTier
    ) {
        return placesService.suggestNeighborhoods(destinationId, budgetTier);
    }

    private double parseCoord(String value, String paramName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Parâmetro obrigatório ausente: " + paramName);
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Parâmetro inválido: " + paramName);
        }
    }
}
