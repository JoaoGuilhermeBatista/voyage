package luvoygroup.voyage.features.billing;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Sem isto, o SDK do Stripe nunca tinha credencial nenhuma configurada —
 * qualquer chamada de saída pra API do Stripe (criar sessão de checkout,
 * buscar detalhes de assinatura) falharia sempre, mesmo com o webhook
 * funcionando perfeitamente pro lado de entrada.
 */
@Configuration
@Slf4j
public class StripeConfig {

    @Value("${stripe.secret-key:not-configured}")
    private String secretKey;

    @PostConstruct
    public void init() {
        if ("not-configured".equals(secretKey)) {
            log.warn("STRIPE_SECRET_KEY não configurada — chamadas ao Stripe (criar checkout, buscar assinatura) vão falhar. Webhooks de entrada continuam funcionando normalmente.");
            return;
        }
        Stripe.apiKey = secretKey;
    }
}
