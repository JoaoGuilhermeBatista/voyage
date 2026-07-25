package luvoygroup.voyage.features.billing;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/stripe")
@RequiredArgsConstructor
@Tag(name = "Billing", description = "Webhooks do Stripe para gestão de assinaturas premium")
public class BillingController {

    private final BillingService billingService;

    @PostMapping("/webhook")
    @Operation(summary = "Recebe eventos do Stripe (ex: checkout.session.completed)")
    public ResponseEntity<Void> webhook(
        @RequestBody String payload,
        @RequestHeader("Stripe-Signature") String sigHeader
    ) {
        billingService.handleWebhook(payload, sigHeader);
        return ResponseEntity.ok().build();
    }
}
