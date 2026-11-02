package luvoygroup.voyage.domain;

import jakarta.persistence.*;
import lombok.*;
import luvoygroup.voyage.domain.enums.DestinationType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "destinations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Destination {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String country;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String tagline;

    /**
     * Cidade, vilarejo, ilha, região, rota ou parque — campo pro filtro de
     * tipo sugerido no Guia Mestre (ex.: Madeira/Creta/Tenerife/Lofoten/
     * Ilhas Faroé/Riviera Albanesa são ilhas ou regiões, não cidades).
     * Default CITY cobre a imensa maioria do catálogo atual.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private DestinationType type = DestinationType.CITY;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String heroImageUrl;

    /**
     * Fotos adicionais pra rotatividade na UI (além da {@link #heroImageUrl},
     * que segue sendo a foto principal/primeira impressão) — mesmo padrão de
     * {@code poi_images} em {@link Poi}, só que no nível de destino.
     */
    /**
     * TEXT (não varchar(255)) de propósito: URLs reais do Wikimedia Commons
     * podem passar de 255 caracteres (nomes de arquivo com acentuação/CJK
     * URL-encoded) — gotcha real encontrado ao rodar o enriquecimento em
     * produção pela 1ª vez (2026-11-02): um INSERT estourando o limite da
     * coluna lançava exceção que "contaminava" a Session do Hibernate
     * (OSIV, 1 Session por request) e travava o restante do lote inteiro
     * com LazyInitializationException no destino seguinte, não só o que
     * falhou.
     */
    @ElementCollection
    @CollectionTable(name = "destination_images", joinColumns = @JoinColumn(name = "destination_id"))
    @Column(name = "image_url", columnDefinition = "TEXT")
    @Builder.Default
    private List<String> images = new ArrayList<>();

    private String signature;

    @ElementCollection
    @CollectionTable(name = "destination_tags", joinColumns = @JoinColumn(name = "destination_id"))
    @Column(name = "tag")
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @OneToMany(mappedBy = "destination", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Poi> pois = new ArrayList<>();

    @OneToMany(mappedBy = "destination", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    @Builder.Default
    private List<DestinationSection> sections = new ArrayList<>();

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
