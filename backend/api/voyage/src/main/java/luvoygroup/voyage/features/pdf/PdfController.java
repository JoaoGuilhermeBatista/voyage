package luvoygroup.voyage.features.pdf;

import luvoygroup.voyage.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pdf")
@RequiredArgsConstructor
public class PdfController {

    private final PdfService pdfService;

    @PostMapping("/export/{itineraryId}")
    public Map<String, String> export(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @PathVariable UUID itineraryId
    ) {
        UUID userId = UUID.fromString(principal.id());
        return pdfService.export(userId, itineraryId);
    }
}
