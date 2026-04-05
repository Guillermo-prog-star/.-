package com.integrityfamily.family.domain;

import com.integrityfamily.auth.domain.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad Family: Representa el Nodo Familiar en el sistema.
 * Centraliza la información del núcleo y su relación con los integrantes.
 */
@Entity
@Table(name = "families")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Family {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "municipio", length = 120)
    private String municipio;

    @Column(name = "whatsapp", length = 30)
    private String whatsapp;

    @Column(name = "pin", length = 4, nullable = false)
    private String pin;

    @Column(name = "family_code", length = 50, unique = true)
    private String familyCode;

    @Column(name = "current_milestone", length = 50)
    private String currentMilestone;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    /**
     * RELACIÓN CON INTEGRANTES:
     * cascade = ALL: Operaciones sobre Family se replican en los Members.
     * orphanRemoval = true: Eliminar de la lista borra el registro en MySQL.
     */
    @OneToMany(mappedBy = "family", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Member> members = new ArrayList<>();

    /**
     * Ciclo de vida JPA: Metadatos automáticos.
     */
    @PrePersist
    public void prePersist() {
        if (this.familyCode == null || this.familyCode.isBlank()) {
            this.familyCode = "FAM-" + System.currentTimeMillis();
        }
        if (this.currentMilestone == null || this.currentMilestone.isBlank()) {
            this.currentMilestone = "DIAGNOSTICO_INICIAL";
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    /**
     * Método de utilidad para añadir miembros asegurando la consistencia.
     */
    public void addMember(Member member) {
        members.add(member);
        member.setFamily(this);
    }
}