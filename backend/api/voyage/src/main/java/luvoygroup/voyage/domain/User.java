package luvoygroup.voyage.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String name;

    private String avatarUrl;

    @Column(nullable = false)
    @Builder.Default
    private boolean onboardingCompleted = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean isPremium = false;

    @Column(nullable = false)
    @Builder.Default
    private int pdfExportCount = 0;

    // Créditos avulsos de exportação de PDF comprados via Pix (item "PDF
    // novo", ver luvoy-pendencias.md) — cada compra soma 1, cada exportação
    // além da 1ª grátis consome 1 crédito antes de bloquear com 402.
    @Column(nullable = false)
    @Builder.Default
    private int extraPdfCredits = 0;

    // Identificadores do Stripe — chave de busca ROBUSTA nos webhooks (em
    // vez de casar só por e-mail, que é frágil). stripeSubscriptionId fica
    // nulo pra quem só usa Pix avulso (nunca tem assinatura de verdade).
    private String stripeCustomerId;
    private String stripeSubscriptionId;

    // Validade do acesso premium. OBRIGATÓRIO sempre que isPremium=true:
    // - Cartão (assinatura Stripe): sincronizado a cada invoice.paid /
    //   customer.subscription.updated com o fim do período de cobrança.
    // - Pix (pagamento avulso mensal, sem auto-renovação possível no
    //   Stripe): setado como "agora + 30 dias" a cada pagamento aprovado.
    // Não existe job/cron pra "expirar" isso — hasActivePremium() abaixo
    // sempre confere a data na hora, então o campo simplesmente para de
    // valer sozinho quando o prazo passa.
    private LocalDateTime premiumExpiresAt;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private OnboardingPreferences preferences;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Fonte única de verdade pra "esse usuário tem acesso premium AGORA" —
     * todo o resto do app (limite de roteiros, exportação de PDF) deve
     * chamar isto, nunca ler {@code isPremium} sozinho. Cobre os dois
     * caminhos de pagamento: cartão (isPremium só é confiável combinado
     * com premiumExpiresAt, sincronizado pelos webhooks de assinatura) e
     * Pix (nunca auto-renova — sem isso, premium via Pix nunca "acabaria"
     * sozinho).
     */
    public boolean hasActivePremium() {
        return isPremium && (premiumExpiresAt == null || premiumExpiresAt.isAfter(LocalDateTime.now()));
    }
}
