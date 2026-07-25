package luvoygroup.voyage.features.recommendations;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import luvoygroup.voyage.features.recommendations.dto.RecommendationDTO;
import luvoygroup.voyage.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
@Tag(name = "Recommendations", description = "Destinos recomendados com base nos interesses do usuário")
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping
    @Operation(summary = "Retorna até 10 destinos rankeados por compatibilidade com o perfil do usuário")
    public List<RecommendationDTO> recommend(@AuthenticationPrincipal AuthenticatedUser principal) {
        return recommendationService.recommend(UUID.fromString(principal.id()));
    }
}
