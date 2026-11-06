package luvoygroup.voyage.features.billing.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * URLs de retorno após o checkout do Stripe (sucesso/cancelamento) —
 * passadas pelo cliente em vez de fixas no backend. BillingService valida
 * que ambas usam o deep-link scheme do app (luvoytravel://), pra não abrir
 * redirect arbitrário vindo do cliente.
 */
public record CheckoutRequest(
    @NotBlank String successUrl,
    @NotBlank String cancelUrl
) {}
