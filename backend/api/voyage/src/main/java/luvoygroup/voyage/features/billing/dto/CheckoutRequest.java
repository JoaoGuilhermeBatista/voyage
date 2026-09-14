package luvoygroup.voyage.features.billing.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * URLs de retorno após o checkout do Stripe (sucesso/cancelamento) —
 * passadas pelo cliente (Flutter) em vez de fixas no backend, porque hoje
 * ainda não existe domínio próprio nem esquema de deep-link definido pro
 * app (ver luvoy-pendencias.md); assim que isso existir, o app só troca o
 * valor que envia aqui, sem precisar mudar o backend.
 */
public record CheckoutRequest(
    @NotBlank String successUrl,
    @NotBlank String cancelUrl
) {}
