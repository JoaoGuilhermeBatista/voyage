package luvoygroup.voyage.domain;

import jakarta.persistence.*;
import lombok.*;
import luvoygroup.voyage.domain.enums.ItinerarySlot;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "itinerary_template_activities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItineraryTemplateActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "itinerary_template_day_id", nullable = false)
    private ItineraryTemplateDay itineraryTemplateDay;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poi_id", nullable = false)
    private Poi poi;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ItinerarySlot slot;

    @Column(name = "order_in_slot", nullable = false)
    private Integer orderInSlot;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
