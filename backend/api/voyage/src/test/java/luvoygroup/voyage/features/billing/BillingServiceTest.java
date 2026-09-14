package luvoygroup.voyage.features.billing;

import com.google.gson.JsonObject;
import com.stripe.exception.ApiConnectionException;
import com.stripe.model.Event;
import com.stripe.model.Subscription;
import com.stripe.net.ApiResource;
import luvoygroup.voyage.domain.ProcessedWebhookEvent;
import luvoygroup.voyage.domain.User;
import luvoygroup.voyage.repository.ProcessedWebhookEventRepository;
import luvoygroup.voyage.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes "standalone" de BillingService, mesmo padrão de ItineraryServiceTest
 * (mocks via Mockito.mock() direto, sem @ExtendWith(MockitoExtension.class)).
 *
 * Foco: a brecha que motivou esta bateria de testes — "cancelar o pagamento
 * e continuar com acesso premium de graça" — e os 3 fluxos de pagamento
 * (cartão-assinatura, Pix-premium-mensal, Pix-PDF-avulso), ver
 * luvoy-pendencias.md.
 *
 * Os objetos Session/Subscription/Invoice são construídos a partir de JSON
 * via {@code ApiResource.GSON} (o mesmo Gson que o próprio SDK usa
 * internamente pra deserializar webhooks reais) — isso exercita o mesmo
 * caminho de código de produção (Event.getDataObjectDeserializer().getObject())
 * em vez de testar contra objetos "de mentirinha" que não passariam pela
 * checagem de versão de API do Stripe.
 */
class BillingServiceTest {

    private UserRepository userRepo;
    private ProcessedWebhookEventRepository processedEventRepo;
    private BillingService service;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String STRIPE_API_VERSION = "2024-06-20"; // com.stripe.net.ApiVersion.CURRENT

    @BeforeEach
    void setUp() {
        userRepo = mock(UserRepository.class);
        processedEventRepo = mock(ProcessedWebhookEventRepository.class);
        service = new BillingService(userRepo, processedEventRepo);
    }

    private User freeUser() {
        return User.builder().id(USER_ID).email("viajante@example.com").isPremium(false)
            .pdfExportCount(0).extraPdfCredits(0).build();
    }

    private Event buildEvent(String eventId, String eventType, String dataObjectJson) {
        JsonObject envelope = new JsonObject();
        envelope.addProperty("id", eventId);
        envelope.addProperty("object", "event");
        envelope.addProperty("api_version", STRIPE_API_VERSION);
        envelope.addProperty("type", eventType);
        JsonObject data = new JsonObject();
        data.add("object", ApiResource.GSON.fromJson(dataObjectJson, JsonObject.class));
        envelope.add("data", data);
        return ApiResource.GSON.fromJson(envelope, Event.class);
    }

    // ─── Idempotência ───────────────────────────────────────────────────────

    @Test
    void processEvent_shouldSkipEntirely_whenEventAlreadyProcessedBefore() {
        when(processedEventRepo.existsById("evt_dup")).thenReturn(true);

        Event event = buildEvent("evt_dup", "checkout.session.completed", """
            {"id":"cs_1","object":"checkout.session","mode":"payment",
             "client_reference_id":"%s","metadata":{"type":"pdf_export_pix"}}
            """.formatted(USER_ID));

        service.processEvent(event);

        verifyNoInteractions(userRepo);
        verify(processedEventRepo, never()).save(any());
    }

    // ─── Pix: premium por 30 dias ───────────────────────────────────────────

    @Test
    void checkoutCompleted_pixPremium_shouldGrantThirtyDaysOfPremium_forNewSubscriber() {
        User user = freeUser();
        when(userRepo.findById(USER_ID)).thenReturn(Optional.of(user));

        Event event = buildEvent("evt_1", "checkout.session.completed", """
            {"id":"cs_1","object":"checkout.session","mode":"payment","customer":"cus_1",
             "client_reference_id":"%s","metadata":{"type":"premium_pix_month"}}
            """.formatted(USER_ID));

        service.processEvent(event);

        assertTrue(user.isPremium());
        assertTrue(user.hasActivePremium());
        assertTrue(user.getPremiumExpiresAt().isAfter(LocalDateTime.now().plusDays(29)));
        assertEquals("cus_1", user.getStripeCustomerId());
        verify(userRepo).save(user);
        verify(processedEventRepo).save(argThat(e -> e.getId().equals("evt_1")));
    }

    @Test
    void checkoutCompleted_pixPremium_shouldStackOnTopOfExistingExpiry_ratherThanReset() {
        User user = freeUser();
        LocalDateTime stillActiveExpiry = LocalDateTime.now().plusDays(10);
        user.setPremium(true);
        user.setPremiumExpiresAt(stillActiveExpiry);
        when(userRepo.findById(USER_ID)).thenReturn(Optional.of(user));

        Event event = buildEvent("evt_2", "checkout.session.completed", """
            {"id":"cs_2","object":"checkout.session","mode":"payment",
             "client_reference_id":"%s","metadata":{"type":"premium_pix_month"}}
            """.formatted(USER_ID));

        service.processEvent(event);

        // 10 dias restantes + 30 novos = ~40, não 30 (não "perde" o que já tinha pago)
        assertTrue(user.getPremiumExpiresAt().isAfter(stillActiveExpiry.plusDays(29)));
    }

    // ─── Pix: crédito avulso de PDF ─────────────────────────────────────────

    @Test
    void checkoutCompleted_pixPdfExport_shouldAddOneCredit_withoutTouchingPremium() {
        User user = freeUser();
        when(userRepo.findById(USER_ID)).thenReturn(Optional.of(user));

        Event event = buildEvent("evt_3", "checkout.session.completed", """
            {"id":"cs_3","object":"checkout.session","mode":"payment",
             "client_reference_id":"%s","metadata":{"type":"pdf_export_pix"}}
            """.formatted(USER_ID));

        service.processEvent(event);

        assertEquals(1, user.getExtraPdfCredits());
        assertFalse(user.isPremium());
    }

    @Test
    void checkoutCompleted_withUnknownMetadataType_shouldGrantNothing_butStillMarkEventProcessed() {
        User user = freeUser();
        when(userRepo.findById(USER_ID)).thenReturn(Optional.of(user));

        Event event = buildEvent("evt_4", "checkout.session.completed", """
            {"id":"cs_4","object":"checkout.session","mode":"payment",
             "client_reference_id":"%s","metadata":{"type":"produto_que_nao_existe"}}
            """.formatted(USER_ID));

        service.processEvent(event);

        assertFalse(user.isPremium());
        assertEquals(0, user.getExtraPdfCredits());
        // Não trava a fila de webhooks por um evento que nunca vai se resolver sozinho.
        verify(processedEventRepo).save(any());
    }

    @Test
    void checkoutCompleted_withNoMatchingUser_shouldLogAndSkip_withoutThrowing() {
        Event event = buildEvent("evt_5", "checkout.session.completed", """
            {"id":"cs_5","object":"checkout.session","mode":"payment",
             "client_reference_id":"%s","metadata":{"type":"pdf_export_pix"}}
            """.formatted(UUID.randomUUID()));

        assertDoesNotThrow(() -> service.processEvent(event));
        verify(userRepo, never()).save(any());
    }

    // ─── Cartão: assinatura via checkout.session.completed (mode=subscription) ─

    @Test
    void checkoutCompleted_subscriptionMode_shouldFetchRealSubscriptionAndGrantPremium() {
        User user = freeUser();
        when(userRepo.findById(USER_ID)).thenReturn(Optional.of(user));

        long periodEnd = LocalDateTime.now().plusDays(30).toEpochSecond(java.time.ZoneOffset.UTC);
        Subscription fakeSubscription = ApiResource.GSON.fromJson(
            """
            {"id":"sub_1","object":"subscription","customer":"cus_1","status":"active","current_period_end":%d}
            """.formatted(periodEnd),
            Subscription.class
        );

        Event event = buildEvent("evt_6", "checkout.session.completed", """
            {"id":"cs_6","object":"checkout.session","mode":"subscription","customer":"cus_1",
             "subscription":"sub_1","client_reference_id":"%s"}
            """.formatted(USER_ID));

        try (MockedStatic<Subscription> mockedSubscription = mockStatic(Subscription.class)) {
            mockedSubscription.when(() -> Subscription.retrieve("sub_1")).thenReturn(fakeSubscription);

            service.processEvent(event);
        }

        assertTrue(user.isPremium());
        assertEquals("sub_1", user.getStripeSubscriptionId());
        assertTrue(user.hasActivePremium());
    }

    @Test
    void checkoutCompleted_subscriptionMode_shouldNotGrantPremium_whenStripeApiCallFails() {
        User user = freeUser();
        when(userRepo.findById(USER_ID)).thenReturn(Optional.of(user));

        Event event = buildEvent("evt_7", "checkout.session.completed", """
            {"id":"cs_7","object":"checkout.session","mode":"subscription","customer":"cus_1",
             "subscription":"sub_1","client_reference_id":"%s"}
            """.formatted(USER_ID));

        try (MockedStatic<Subscription> mockedSubscription = mockStatic(Subscription.class)) {
            mockedSubscription.when(() -> Subscription.retrieve("sub_1"))
                .thenThrow(new ApiConnectionException("Stripe fora do ar"));

            // Não deve propagar — o evento é logado como precisando de correção manual,
            // não derruba o processamento do webhook inteiro.
            assertDoesNotThrow(() -> service.processEvent(event));
        }

        assertFalse(user.isPremium());
    }

    // ─── invoice.paid (renovação de assinatura) ────────────────────────────

    @Test
    void invoicePaid_shouldExtendPremiumExpiry_forExistingSubscriber() {
        User user = freeUser();
        user.setPremium(true);
        user.setStripeCustomerId("cus_1");
        user.setStripeSubscriptionId("sub_1");
        user.setPremiumExpiresAt(LocalDateTime.now().minusDays(1)); // tinha acabado de expirar
        when(userRepo.findByStripeCustomerId("cus_1")).thenReturn(Optional.of(user));

        long newPeriodEnd = LocalDateTime.now().plusDays(30).toEpochSecond(java.time.ZoneOffset.UTC);
        Subscription fakeSubscription = ApiResource.GSON.fromJson(
            """
            {"id":"sub_1","object":"subscription","customer":"cus_1","status":"active","current_period_end":%d}
            """.formatted(newPeriodEnd),
            Subscription.class
        );

        Event event = buildEvent("evt_8", "invoice.paid", """
            {"id":"in_1","object":"invoice","customer":"cus_1","subscription":"sub_1"}
            """);

        try (MockedStatic<Subscription> mockedSubscription = mockStatic(Subscription.class)) {
            mockedSubscription.when(() -> Subscription.retrieve("sub_1")).thenReturn(fakeSubscription);
            service.processEvent(event);
        }

        assertTrue(user.hasActivePremium());
    }

    @Test
    void invoicePaid_shouldBeIgnored_whenInvoiceHasNoSubscription() {
        Event event = buildEvent("evt_9", "invoice.paid", """
            {"id":"in_2","object":"invoice","customer":"cus_1"}
            """);

        assertDoesNotThrow(() -> service.processEvent(event));
        verifyNoInteractions(userRepo);
    }

    // ─── invoice.payment_failed — NÃO deve revogar acesso na hora ──────────

    @Test
    void invoicePaymentFailed_shouldNotRevokeAccess_onFirstFailure() {
        Event event = buildEvent("evt_10", "invoice.payment_failed", """
            {"id":"in_3","object":"invoice","customer":"cus_1","subscription":"sub_1"}
            """);

        service.processEvent(event);

        // Deliberadamente não mexe em nada — Stripe tenta cobrar de novo sozinho.
        verifyNoInteractions(userRepo);
    }

    // ─── customer.subscription.deleted — a checagem central deste teste ────

    @Test
    void subscriptionDeleted_shouldKeepPremiumAccess_untilTheAlreadyPaidPeriodActuallyEnds() {
        // Cenário exato pedido pelo usuário: assinou 14/09 (period end
        // 14/10), cancelou 18/09 — tem que continuar premium até 14/10.
        User user = freeUser();
        user.setPremium(true);
        user.setStripeSubscriptionId("sub_1");
        LocalDateTime periodEnd = LocalDateTime.now().plusDays(26); // "14/10" relativo a um cancelamento em "18/09"
        user.setPremiumExpiresAt(periodEnd);
        when(userRepo.findByStripeSubscriptionId("sub_1")).thenReturn(Optional.of(user));

        Event event = buildEvent("evt_11", "customer.subscription.deleted", """
            {"id":"sub_1","object":"subscription","customer":"cus_1","status":"canceled"}
            """);

        service.processEvent(event);

        assertTrue(user.hasActivePremium(), "cancelar não deve derrubar o acesso antes do fim do período já pago");
        assertEquals(periodEnd, user.getPremiumExpiresAt(), "a data de validade não deve ser alterada pelo cancelamento");
        assertNull(user.getStripeSubscriptionId(), "mas para de ser tratada como assinatura ativa (não sincroniza mais)");
    }

    @Test
    void subscriptionDeleted_thenTimePasses_accessExpiresNaturally_withNoCronNeeded() {
        User user = freeUser();
        user.setPremium(true);
        user.setStripeSubscriptionId("sub_1");
        user.setPremiumExpiresAt(LocalDateTime.now().plusSeconds(1));
        when(userRepo.findByStripeSubscriptionId("sub_1")).thenReturn(Optional.of(user));

        service.processEvent(buildEvent("evt_11b", "customer.subscription.deleted", """
            {"id":"sub_1","object":"subscription","customer":"cus_1","status":"canceled"}
            """));
        assertTrue(user.hasActivePremium());

        try {
            Thread.sleep(1100);
        } catch (InterruptedException ignored) {
        }

        assertFalse(user.hasActivePremium(), "hasActivePremium() sozinho já reflete o vencimento, sem precisar de nenhum job");
    }

    @Test
    void subscriptionDeleted_withNoMatchingUser_shouldNotThrow() {
        Event event = buildEvent("evt_12", "customer.subscription.deleted", """
            {"id":"sub_missing","object":"subscription","customer":"cus_missing","status":"canceled"}
            """);

        assertDoesNotThrow(() -> service.processEvent(event));
    }

    // ─── customer.subscription.updated — segunda linha de defesa ───────────

    @Test
    void subscriptionUpdated_shouldRevokePremium_whenStatusBecomesUnpaid() {
        User user = freeUser();
        user.setPremium(true);
        user.setStripeSubscriptionId("sub_1");
        user.setPremiumExpiresAt(LocalDateTime.now().plusDays(20));
        when(userRepo.findByStripeSubscriptionId("sub_1")).thenReturn(Optional.of(user));

        Event event = buildEvent("evt_13", "customer.subscription.updated", """
            {"id":"sub_1","object":"subscription","customer":"cus_1","status":"unpaid","current_period_end":0}
            """);

        service.processEvent(event);

        assertFalse(user.isPremium());
    }

    @Test
    void subscriptionUpdated_shouldKeepPremiumActive_whenStatusIsStillPastDue() {
        User user = freeUser();
        user.setPremium(true);
        user.setStripeSubscriptionId("sub_1");
        LocalDateTime originalExpiry = LocalDateTime.now().plusDays(20);
        user.setPremiumExpiresAt(originalExpiry);
        when(userRepo.findByStripeSubscriptionId("sub_1")).thenReturn(Optional.of(user));

        Event event = buildEvent("evt_14", "customer.subscription.updated", """
            {"id":"sub_1","object":"subscription","customer":"cus_1","status":"past_due","current_period_end":0}
            """);

        service.processEvent(event);

        // Ainda em janela de retentativa do Stripe — não perde acesso, e o
        // save() nem chega a ser chamado (early return, sem alteração nenhuma).
        assertTrue(user.isPremium());
        assertEquals(originalExpiry, user.getPremiumExpiresAt());
        verify(userRepo, never()).save(any());
    }

    @Test
    void subscriptionUpdated_statusCanceled_shouldAlsoKeepAccessUntilExpiry_sameAsSubscriptionDeleted() {
        User user = freeUser();
        user.setPremium(true);
        user.setStripeSubscriptionId("sub_1");
        LocalDateTime periodEnd = LocalDateTime.now().plusDays(26);
        user.setPremiumExpiresAt(periodEnd);
        when(userRepo.findByStripeSubscriptionId("sub_1")).thenReturn(Optional.of(user));

        Event event = buildEvent("evt_16", "customer.subscription.updated", """
            {"id":"sub_1","object":"subscription","customer":"cus_1","status":"canceled","current_period_end":0}
            """);

        service.processEvent(event);

        assertTrue(user.hasActivePremium());
        assertEquals(periodEnd, user.getPremiumExpiresAt());
    }

    @Test
    void subscriptionUpdated_shouldResyncExpiryAndReactivate_whenStatusGoesBackToActive() {
        User user = freeUser();
        user.setPremium(false); // ex.: tinha caído por engano, ou reativou depois de "unpaid"
        user.setStripeSubscriptionId("sub_1");
        when(userRepo.findByStripeSubscriptionId("sub_1")).thenReturn(Optional.of(user));

        long periodEnd = LocalDateTime.now().plusDays(30).toEpochSecond(java.time.ZoneOffset.UTC);
        Event event = buildEvent("evt_15", "customer.subscription.updated", """
            {"id":"sub_1","object":"subscription","customer":"cus_1","status":"active","current_period_end":%d}
            """.formatted(periodEnd));

        service.processEvent(event);

        assertTrue(user.hasActivePremium());
    }

    // ─── Criação de checkout ────────────────────────────────────────────────

    @Test
    void createPixPdfExportCheckout_shouldSetClientReferenceIdAndMetadata_andReuseExistingCustomer() {
        User user = freeUser();
        user.setStripeCustomerId("cus_existing");
        when(userRepo.findById(USER_ID)).thenReturn(Optional.of(user));

        try (MockedStatic<com.stripe.model.checkout.Session> mockedSession =
                 mockStatic(com.stripe.model.checkout.Session.class)) {
            var fakeSession = new com.stripe.model.checkout.Session();
            fakeSession.setUrl("https://checkout.stripe.com/pay/cs_test_123");

            ArgumentCaptor<com.stripe.param.checkout.SessionCreateParams> captor =
                ArgumentCaptor.forClass(com.stripe.param.checkout.SessionCreateParams.class);
            mockedSession.when(() -> com.stripe.model.checkout.Session.create(captor.capture()))
                .thenReturn(fakeSession);

            var result = service.createPixPdfExportCheckout(USER_ID, "https://app/success", "https://app/cancel");

            assertEquals("https://checkout.stripe.com/pay/cs_test_123", result.checkoutUrl());
            var params = captor.getValue();
            assertEquals(USER_ID.toString(), params.getClientReferenceId());
            assertEquals("cus_existing", params.getCustomer());
            assertNull(params.getCustomerEmail());
            assertEquals("pdf_export_pix", params.getMetadata().get("type"));
        }
    }

    @Test
    void createSubscriptionCheckout_shouldUseCustomerEmail_whenUserHasNoStripeCustomerIdYet() {
        User user = freeUser(); // sem stripeCustomerId
        when(userRepo.findById(USER_ID)).thenReturn(Optional.of(user));

        try (MockedStatic<com.stripe.model.checkout.Session> mockedSession =
                 mockStatic(com.stripe.model.checkout.Session.class)) {
            var fakeSession = new com.stripe.model.checkout.Session();
            fakeSession.setUrl("https://checkout.stripe.com/pay/cs_test_456");

            ArgumentCaptor<com.stripe.param.checkout.SessionCreateParams> captor =
                ArgumentCaptor.forClass(com.stripe.param.checkout.SessionCreateParams.class);
            mockedSession.when(() -> com.stripe.model.checkout.Session.create(captor.capture()))
                .thenReturn(fakeSession);

            service.createSubscriptionCheckout(USER_ID, "https://app/success", "https://app/cancel");

            var params = captor.getValue();
            assertEquals("viajante@example.com", params.getCustomerEmail());
            assertNull(params.getCustomer());
            assertEquals(com.stripe.param.checkout.SessionCreateParams.Mode.SUBSCRIPTION, params.getMode());
        }
    }
}
