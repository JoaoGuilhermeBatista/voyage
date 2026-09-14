package luvoygroup.voyage.features.billing;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.Invoice;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import luvoygroup.voyage.domain.ProcessedWebhookEvent;
import luvoygroup.voyage.domain.User;
import luvoygroup.voyage.exception.ResourceNotFoundException;
import luvoygroup.voyage.features.billing.dto.CheckoutSessionDTO;
import luvoygroup.voyage.repository.ProcessedWebhookEventRepository;
import luvoygroup.voyage.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

/**
 * 3 formas de pagamento, ver luvoy-pendencias.md:
 * 1) Cartão → assinatura Stripe recorrente mensal (renova sozinha).
 * 2) Pix → pagamento avulso que dá 30 dias de premium (Pix NÃO suporta
 *    cobrança recorrente automática no Stripe — cada mês é um pagamento
 *    novo, o usuário paga de novo quando quiser renovar).
 * 3) Pix → pagamento avulso de 1 exportação extra de PDF.
 *
 * Todo estado de acesso (User.isPremium/premiumExpiresAt/extraPdfCredits)
 * só é alterado a partir de eventos de webhook confirmados pelo Stripe
 * (assinatura verificada) — nunca a partir de algo que o cliente Flutter
 * afirma diretamente, pra não abrir brecha de "fingir que pagou".
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BillingService {

    private final UserRepository userRepo;
    private final ProcessedWebhookEventRepository processedEventRepo;

    @Value("${stripe.webhook-secret:not-configured}")
    private String webhookSecret;

    @Value("${stripe.price.premium-monthly-card:not-configured}")
    private String priceIdPremiumCard;

    @Value("${stripe.price.premium-monthly-pix:not-configured}")
    private String priceIdPremiumPix;

    @Value("${stripe.price.pdf-export-pix:not-configured}")
    private String priceIdPdfExportPix;

    // ─── Criação de checkout (o app chama isso pra começar um pagamento) ──────

    public CheckoutSessionDTO createSubscriptionCheckout(UUID userId, String successUrl, String cancelUrl) {
        User user = userRepo.findById(userId).orElseThrow(ResourceNotFoundException::new);
        return new CheckoutSessionDTO(createSession(
            user, SessionCreateParams.Mode.SUBSCRIPTION, SessionCreateParams.PaymentMethodType.CARD,
            priceIdPremiumCard, null, successUrl, cancelUrl
        ));
    }

    public CheckoutSessionDTO createPixPremiumCheckout(UUID userId, String successUrl, String cancelUrl) {
        User user = userRepo.findById(userId).orElseThrow(ResourceNotFoundException::new);
        return new CheckoutSessionDTO(createSession(
            user, SessionCreateParams.Mode.PAYMENT, SessionCreateParams.PaymentMethodType.PIX,
            priceIdPremiumPix, "premium_pix_month", successUrl, cancelUrl
        ));
    }

    public CheckoutSessionDTO createPixPdfExportCheckout(UUID userId, String successUrl, String cancelUrl) {
        User user = userRepo.findById(userId).orElseThrow(ResourceNotFoundException::new);
        return new CheckoutSessionDTO(createSession(
            user, SessionCreateParams.Mode.PAYMENT, SessionCreateParams.PaymentMethodType.PIX,
            priceIdPdfExportPix, "pdf_export_pix", successUrl, cancelUrl
        ));
    }

    private String createSession(
        User user, SessionCreateParams.Mode mode, SessionCreateParams.PaymentMethodType paymentMethodType,
        String priceId, String productType, String successUrl, String cancelUrl
    ) {
        var builder = SessionCreateParams.builder()
            .setMode(mode)
            .addPaymentMethodType(paymentMethodType)
            .addLineItem(SessionCreateParams.LineItem.builder()
                .setPrice(priceId)
                .setQuantity(1L)
                .build())
            // client_reference_id é a forma robusta de casar o pagamento com
            // o usuário no webhook — independe de e-mail bater e sobrevive
            // mesmo antes do usuário ter um stripeCustomerId ainda.
            .setClientReferenceId(user.getId().toString())
            .setSuccessUrl(successUrl)
            .setCancelUrl(cancelUrl);

        // Reaproveita o Customer já existente no Stripe em vez de criar um
        // novo a cada compra (evita duplicar cliente pra quem já pagou antes).
        if (user.getStripeCustomerId() != null) {
            builder.setCustomer(user.getStripeCustomerId());
        } else {
            builder.setCustomerEmail(user.getEmail());
        }

        if (productType != null) {
            builder.putMetadata("type", productType);
        }

        try {
            Session session = Session.create(builder.build());
            return session.getUrl();
        } catch (StripeException e) {
            log.error("Falha ao criar sessão de checkout no Stripe para o usuário {}: {}", user.getId(), e.getMessage());
            throw new IllegalStateException("Não foi possível iniciar o pagamento. Tente novamente em instantes.");
        }
    }

    // ─── Webhook (o Stripe chama isso quando algo acontece do lado dele) ──────

    @Transactional
    public void handleWebhook(String payload, String sigHeader) {
        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            throw new IllegalArgumentException("Stripe signature inválida");
        }
        processEvent(event);
    }

    /**
     * Separado de {@link #handleWebhook} (que cuida só da verificação de
     * assinatura) pra dar pra testar toda a lógica de negócio — idempotência
     * + roteamento por tipo + cada handler — com um {@link Event} montado à
     * mão em teste, sem precisar simular uma assinatura HMAC válida.
     */
    void processEvent(Event event) {
        // Idempotência: o Stripe pode reentregar o mesmo evento mais de uma
        // vez — sem isto, um "checkout.session.completed" duplicado
        // creditaria o mesmo pagamento 2x (2 meses de premium, 2 créditos
        // de PDF etc.).
        if (processedEventRepo.existsById(event.getId())) {
            log.info("Evento Stripe {} já processado antes, ignorando reentrega.", event.getId());
            return;
        }

        var dataObject = event.getDataObjectDeserializer().getObject().orElse(null);
        if (dataObject == null) {
            log.warn("Evento Stripe {} ({}) sem payload deserializável, ignorando.", event.getId(), event.getType());
            processedEventRepo.save(new ProcessedWebhookEvent(event.getId()));
            return;
        }

        try {
            switch (event.getType()) {
                case "checkout.session.completed" -> handleCheckoutCompleted((Session) dataObject);
                case "invoice.paid" -> handleInvoicePaid((Invoice) dataObject);
                case "invoice.payment_failed" -> handleInvoicePaymentFailed((Invoice) dataObject);
                case "customer.subscription.deleted" -> handleSubscriptionDeleted((Subscription) dataObject);
                case "customer.subscription.updated" -> handleSubscriptionUpdated((Subscription) dataObject);
                default -> log.debug("Evento Stripe {} ignorado (tipo não tratado por esta integração).", event.getType());
            }
        } catch (Exception e) {
            // Qualquer erro inesperado aqui (bug nosso, Stripe API fora do
            // ar na hora de buscar detalhes da assinatura etc.) propaga —
            // o evento NÃO é marcado como processado, e o Stripe reenvia
            // automaticamente mais tarde. Só o que é "resolvido/ignorado
            // de propósito" (usuário não encontrado, produto desconhecido)
            // é tratado dentro de cada handler sem lançar exceção.
            log.error("Erro processando evento Stripe {} ({}): {}", event.getId(), event.getType(), e.getMessage(), e);
            throw e;
        }

        processedEventRepo.save(new ProcessedWebhookEvent(event.getId()));
    }

    // ─── Handlers por tipo de evento ──────────────────────────────────────────

    void handleCheckoutCompleted(Session session) {
        Optional<User> maybeUser = resolveUser(session.getClientReferenceId(), session.getCustomerEmail());
        if (maybeUser.isEmpty()) {
            log.error("checkout.session.completed sem usuário correspondente (client_reference_id={}, email={}) — nenhum acesso foi concedido.",
                session.getClientReferenceId(), session.getCustomerEmail());
            return;
        }
        User user = maybeUser.get();

        if (session.getCustomer() != null) {
            user.setStripeCustomerId(session.getCustomer());
        }

        if ("subscription".equals(session.getMode())) {
            grantSubscriptionAccess(user, session.getSubscription());
        } else if ("payment".equals(session.getMode())) {
            String type = session.getMetadata() != null ? session.getMetadata().get("type") : null;
            switch (type == null ? "" : type) {
                case "premium_pix_month" -> {
                    LocalDateTime base = user.hasActivePremium() ? user.getPremiumExpiresAt() : LocalDateTime.now();
                    user.setPremium(true);
                    user.setPremiumExpiresAt(base.plusDays(30));
                    log.info("Premium via Pix concedido/renovado para {} até {}", user.getId(), user.getPremiumExpiresAt());
                }
                case "pdf_export_pix" -> {
                    user.setExtraPdfCredits(user.getExtraPdfCredits() + 1);
                    log.info("Crédito extra de PDF via Pix concedido para {} (total agora: {})", user.getId(), user.getExtraPdfCredits());
                }
                default -> log.error("checkout.session.completed em modo payment com metadata.type desconhecido ou ausente ('{}') para usuário {} — nenhum benefício foi concedido, pagamento pode precisar de estorno manual.",
                    type, user.getId());
            }
        }

        userRepo.save(user);
    }

    /** Busca detalhes reais da assinatura no Stripe pra saber até quando ela vale — não confia em nenhuma data vinda só do evento de checkout. */
    void grantSubscriptionAccess(User user, String subscriptionId) {
        try {
            Subscription subscription = Subscription.retrieve(subscriptionId);
            user.setStripeSubscriptionId(subscriptionId);
            user.setPremium(true);
            user.setPremiumExpiresAt(epochSecondsToLocalDateTime(currentPeriodEnd(subscription)));
            log.info("Assinatura {} ativada para o usuário {}, válida até {}", subscriptionId, user.getId(), user.getPremiumExpiresAt());
        } catch (StripeException e) {
            log.error("checkout.session.completed (assinatura) mas falhou ao buscar detalhes da subscription {} no Stripe: {} — usuário {} NÃO foi marcado como premium, precisa de correção manual.",
                subscriptionId, e.getMessage(), user.getId());
        }
    }

    void handleInvoicePaid(Invoice invoice) {
        if (invoice.getSubscription() == null) return; // fatura avulsa, não é renovação de assinatura

        Optional<User> maybeUser = userRepo.findByStripeCustomerId(invoice.getCustomer());
        if (maybeUser.isEmpty()) {
            log.warn("invoice.paid para customer {} sem usuário correspondente — ignorado.", invoice.getCustomer());
            return;
        }
        User user = maybeUser.get();
        grantSubscriptionAccess(user, invoice.getSubscription());
        userRepo.save(user);
    }

    /**
     * Deliberadamente NÃO revoga o acesso na primeira falha de cobrança —
     * o Stripe já tenta cobrar de novo automaticamente (Smart Retries) por
     * conta própria durante alguns dias. Cortar o acesso já na primeira
     * tentativa falhada puniria gente por uma falha passageira de cartão.
     * O corte de verdade acontece em handleSubscriptionDeleted/Updated,
     * quando o Stripe desiste de vez.
     */
    void handleInvoicePaymentFailed(Invoice invoice) {
        log.warn("Falha de cobrança na assinatura do customer {} (invoice {}) — acesso mantido, Stripe vai tentar cobrar de novo automaticamente.",
            invoice.getCustomer(), invoice.getId());
    }

    /**
     * IMPORTANTE (decisão do usuário, 2026-09-14): cancelar NÃO derruba o
     * acesso na hora — o período já foi pago, então o acesso continua até
     * {@code premiumExpiresAt} (ex.: assinou dia 14/09, cancelou dia 18/09,
     * mantém premium até 14/10). Por isso NÃO mexemos em isPremium nem em
     * premiumExpiresAt aqui — só paramos de tratar como assinatura ativa
     * (limpando stripeSubscriptionId, pra não tentar sincronizar de novo).
     * hasActivePremium() já cuida sozinho de "desligar" o acesso quando a
     * data passar — mesmíssimo critério que o Pix já usa.
     */
    void handleSubscriptionDeleted(Subscription subscription) {
        Optional<User> maybeUser = resolveUserBySubscriptionOrCustomer(subscription);
        if (maybeUser.isEmpty()) {
            log.warn("customer.subscription.deleted para subscription {} sem usuário correspondente — ignorado.", subscription.getId());
            return;
        }
        User user = maybeUser.get();
        user.setStripeSubscriptionId(null);
        userRepo.save(user);
        log.info("Assinatura {} cancelada para o usuário {} — acesso premium mantido até o fim do período já pago ({}).",
            subscription.getId(), user.getId(), user.getPremiumExpiresAt());
    }

    private static final java.util.Set<String> ACTIVE_SUBSCRIPTION_STATUSES = java.util.Set.of("active", "trialing");
    // "canceled" aqui recebe o MESMO tratamento de handleSubscriptionDeleted
    // (mantém acesso até premiumExpiresAt) — só "unpaid"/"incomplete_expired"
    // revogam na hora, porque aí não existe "período já pago" pra honrar:
    // o Stripe esgotou as tentativas de cobrança do período atual sem
    // sucesso nenhum.
    private static final java.util.Set<String> PAYMENT_FAILURE_EXHAUSTED_STATUSES = java.util.Set.of("unpaid", "incomplete_expired");

    /**
     * Segunda linha de defesa: mesmo que por algum motivo o evento
     * subscription.deleted se perca, qualquer atualização de status da
     * assinatura já resincroniza o acesso com a realidade do Stripe.
     */
    void handleSubscriptionUpdated(Subscription subscription) {
        Optional<User> maybeUser = resolveUserBySubscriptionOrCustomer(subscription);
        if (maybeUser.isEmpty()) {
            log.warn("customer.subscription.updated para subscription {} sem usuário correspondente — ignorado.", subscription.getId());
            return;
        }
        User user = maybeUser.get();
        String status = subscription.getStatus();

        if (ACTIVE_SUBSCRIPTION_STATUSES.contains(status)) {
            user.setPremium(true);
            user.setStripeSubscriptionId(subscription.getId());
            user.setPremiumExpiresAt(epochSecondsToLocalDateTime(currentPeriodEnd(subscription)));
        } else if (PAYMENT_FAILURE_EXHAUSTED_STATUSES.contains(status)) {
            user.setPremium(false);
            user.setStripeSubscriptionId(null);
        } else if ("canceled".equals(status)) {
            user.setStripeSubscriptionId(null); // mantém acesso até premiumExpiresAt, ver handleSubscriptionDeleted
        } else {
            // ex.: "past_due" — ainda em janela de retentativa, não mexe no acesso.
            log.info("Assinatura {} do usuário {} mudou para status '{}' — sem alteração de acesso.", subscription.getId(), user.getId(), status);
            return;
        }
        userRepo.save(user);
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private Optional<User> resolveUser(String clientReferenceId, String customerEmail) {
        if (clientReferenceId != null) {
            try {
                Optional<User> byId = userRepo.findById(UUID.fromString(clientReferenceId));
                if (byId.isPresent()) return byId;
            } catch (IllegalArgumentException ignored) {
                // client_reference_id não era um UUID válido — cai pro fallback de e-mail
            }
        }
        return customerEmail != null ? userRepo.findByEmail(customerEmail) : Optional.empty();
    }

    private Optional<User> resolveUserBySubscriptionOrCustomer(Subscription subscription) {
        Optional<User> bySubscription = userRepo.findByStripeSubscriptionId(subscription.getId());
        if (bySubscription.isPresent()) return bySubscription;
        return subscription.getCustomer() != null
            ? userRepo.findByStripeCustomerId(subscription.getCustomer())
            : Optional.empty();
    }

    private long currentPeriodEnd(Subscription subscription) {
        return subscription.getCurrentPeriodEnd();
    }

    private LocalDateTime epochSecondsToLocalDateTime(long epochSeconds) {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), ZoneOffset.UTC);
    }
}
