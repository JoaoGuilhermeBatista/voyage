package luvoygroup.voyage.features.billing;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import luvoygroup.voyage.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class BillingService {

    private final UserRepository userRepo;

    @Value("${stripe.webhook-secret:not-configured}")
    private String webhookSecret;

    @Transactional
    public void handleWebhook(String payload, String sigHeader) {
        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            throw new IllegalArgumentException("Stripe signature inválida");
        }

        if ("checkout.session.completed".equals(event.getType())) {
            var dataObject = event.getDataObjectDeserializer()
                .getObject()
                .orElseThrow(() -> new IllegalStateException("Payload do evento Stripe inválido"));

            Session session = (Session) dataObject;
            String customerEmail = session.getCustomerEmail() != null
                ? session.getCustomerEmail()
                : (session.getCustomerDetails() != null ? session.getCustomerDetails().getEmail() : null);

            if (customerEmail == null) {
                log.warn("Webhook Stripe: checkout.session.completed sem email de cliente — session {}", session.getId());
                return;
            }

            userRepo.findByEmail(customerEmail).ifPresentOrElse(user -> {
                user.setPremium(true);
                userRepo.save(user);
                log.info("Usuário {} ativado como premium via Stripe", customerEmail);
            }, () -> log.warn("Webhook Stripe: usuário não encontrado para email {}", customerEmail));
        }
    }
}
