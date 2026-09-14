package luvoygroup.voyage.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Registro de deduplicação de webhooks do Stripe — o Stripe pode reenviar o
 * mesmo evento mais de uma vez (rede instável, timeout na nossa resposta
 * etc.), e sem isso um "checkout.session.completed" duplicado creditaria
 * 2 meses de premium ou 2 créditos de PDF pelo mesmo pagamento único.
 * A chave é o próprio {@code event.id} do Stripe (já é globalmente único).
 */
@Entity
@Table(name = "processed_webhook_events")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedWebhookEvent {

    @Id
    private String id;

    private LocalDateTime processedAt;

    public ProcessedWebhookEvent(String id) {
        this.id = id;
        this.processedAt = LocalDateTime.now();
    }
}
