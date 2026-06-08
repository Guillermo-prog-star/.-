package com.integrityfamily.dna.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "family_dna")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FamilyDna {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "family_id", nullable = false, unique = true)
    private Long familyId;

    @Column(columnDefinition = "JSON")
    private String valores;

    @Column(columnDefinition = "JSON")
    private String fortalezas;

    @Column(columnDefinition = "JSON")
    private String sombras;

    @Column(columnDefinition = "JSON")
    private String patrones;

    @Column(name = "estilo_comunicacion", columnDefinition = "TEXT")
    private String estiloComunicacion;

    @Column(name = "ritmo_familiar", columnDefinition = "TEXT")
    private String ritmoFamiliar;

    @Column(name = "potencial_oculto", columnDefinition = "JSON")
    private String potencialOculto;

    @Column(name = "narrativa_ia", columnDefinition = "TEXT")
    private String narrativaIa;

    @Builder.Default
    @Column(nullable = false)
    private Integer version = 1;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (version == null) version = 1;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
        if (version == null) version = 1;
        else version++;
    }
}
