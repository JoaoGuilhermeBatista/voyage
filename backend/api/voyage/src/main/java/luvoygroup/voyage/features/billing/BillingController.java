package luvoygroup.voyage.features.billing;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import luvoygroup.voyage.features.billing.dto.CheckoutRequest;
import luvoygroup.voyage.features.billing.dto.CheckoutSessionDTO;
import luvoygroup.voyage.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Billing", description = "Assinatura Premium (cartão/Pix) e exportação avulsa de PDF (Pix), + webhook do Stripe")
public class BillingController {

    private final BillingService billingService;

    @PostMapping("/billing/checkout/subscription")
    @Operation(summary = "Cria checkout de assinatura Premium recorrente via cartão")
    public CheckoutSessionDTO createSubscriptionCheckout(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @RequestBody @Valid CheckoutRequest request
    ) {
        return billingService.createSubscriptionCheckout(
            UUID.fromString(principal.id()), request.successUrl(), request.cancelUrl()
        );
    }

    @PostMapping("/billing/checkout/pix-premium")
    @Operation(summary = "Cria checkout de 30 dias de Premium via Pix (pagamento único, sem auto-renovação)")
    public CheckoutSessionDTO createPixPremiumCheckout(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @RequestBody @Valid CheckoutRequest request
    ) {
        return billingService.createPixPremiumCheckout(
            UUID.fromString(principal.id()), request.successUrl(), request.cancelUrl()
        );
    }

    @PostMapping("/billing/checkout/pix-pdf-export")
    @Operation(summary = "Cria checkout de 1 crédito avulso de exportação de PDF via Pix")
    public CheckoutSessionDTO createPixPdfExportCheckout(
        @AuthenticationPrincipal AuthenticatedUser principal,
        @RequestBody @Valid CheckoutRequest request
    ) {
        return billingService.createPixPdfExportCheckout(
            UUID.fromString(principal.id()), request.successUrl(), request.cancelUrl()
        );
    }

    @PostMapping("/stripe/webhook")
    @Operation(summary = "Recebe eventos do Stripe (checkout concluído, renovação, cancelamento de assinatura etc.)")
    public ResponseEntity<Void> webhook(
        @RequestBody String payload,
        @RequestHeader("Stripe-Signature") String sigHeader
    ) {
        billingService.handleWebhook(payload, sigHeader);
        return ResponseEntity.ok().build();
    }
}
