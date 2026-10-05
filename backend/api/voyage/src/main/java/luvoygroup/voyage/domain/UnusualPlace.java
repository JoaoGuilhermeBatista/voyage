package luvoygroup.voyage.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lugar fora do roteiro turístico óbvio de um destino — conteúdo editorial
 * curado via LLM (ver {@code LlmService#generateUnusualPlaces}), não um
 * {@link Poi} oficial (sem lat/lng obrigatório, sem categoria fechada).
 * Feature premium: o acesso ao conteúdo completo é gated por
 * {@link User#hasActivePremium()} na camada de serviço, não aqui.
 */
@Entity
@Table(name = "unusual_places")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnusualPlace {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_id", nullable = false)
    private Destination destination;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    private String category;

    @Column(nullable = false)
    private Integer orderIndex;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
