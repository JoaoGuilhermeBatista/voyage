package luvoygroup.voyage.features.unusualplaces;

import luvoygroup.voyage.features.unusualplaces.dto.UnusualPlaceDTO;
import luvoygroup.voyage.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Deliberadamente montado em "/api/v1/unusual-places/{slug}" e não em
 * "/api/v1/destinations/{slug}/...": o padrão
 * "GET /api/v1/destinations/**" é permitAll em SecurityConfig, e este
 * endpoint precisa do principal autenticado pra decidir premium — um path
 * fora desse prefixo cai no "anyRequest().authenticated()" padrão, sem
 * precisar tocar na config de segurança existente.
 */
@RestController
@RequestMapping("/api/v1/unusual-places")
@RequiredArgsConstructor
public class UnusualPlaceController {

    private final UnusualPlaceService unusualPlaceService;

    @GetMapping("/{slug}")
    public List<UnusualPlaceDTO> getForDestination(
        @PathVariable String slug,
        @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        return unusualPlaceService.findForUser(slug, UUID.fromString(principal.id()));
    }
}
