package luvoygroup.voyage.domain;

import jakarta.persistence.*;
import lombok.*;
import luvoygroup.voyage.domain.enums.BudgetTier;
import luvoygroup.voyage.domain.enums.PoiCategory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "pois")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Poi {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_id", nullable = false)
    private Destination destination;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PoiCategory category;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String address;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @ElementCollection
    @CollectionTable(name = "poi_images", joinColumns = @JoinColumn(name = "poi_id"))
    @Column(name = "image_url")
    @Builder.Default
    private List<String> images = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "poi_tags", joinColumns = @JoinColumn(name = "poi_id"))
    @Column(name = "tag")
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BudgetTier budgetTier;

    private LocalTime openingTime;

    private LocalTime closingTime;

    private Integer averageDurationMinutes;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "must_see", nullable = false)
    @Builder.Default
    private boolean mustSee = false;

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
