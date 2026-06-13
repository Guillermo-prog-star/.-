package com.integrityfamily.lineage.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lineage_members")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class LineageMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lineage_id", nullable = false)
    @JsonIgnore
    private FamilyLineage lineage;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "avatar_initials", length = 5)
    private String avatarInitials;

    @Column(name = "avatar_color")
    private String avatarColor;

    /**
     * Posición relativa al ancla del árbol.
     * -3=tatarabuelos, -2=bisabuelos, -1=abuelos,
     *  0=generación responsable (ancla),
     * +1=hijos/generación actual, +2=nietos, +3=bisnietos
     */
    @Column(nullable = false)
    private Integer generation;

    /**
     * Tipo semántico: founding | builder | responsible | current | future | projected
     */
    @Column(name = "generation_type", length = 30)
    private String generationType;

    /** true si este miembro es el nodo ancla desde donde se construyó el árbol */
    @Column(name = "is_anchor", nullable = false)
    @Builder.Default
    private Boolean isAnchor = false;

    /** alive | deceased | unknown | future */
    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "birth_year")
    private Integer birthYear;

    @Column(name = "birth_year_approximate")
    private Boolean birthYearApproximate;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "death_year")
    private Integer deathYear;

    @Column(name = "death_date")
    private LocalDate deathDate;

    @Column
    private String origin;

    @Column(name = "role_label")
    private String roleLabel;

    @Column(name = "confidence_level")
    private Integer confidenceLevel;

    @Column(name = "data_source")
    private String dataSource;

    /** Historia personal narrativa */
    @Column(columnDefinition = "TEXT")
    private String story;

    /** Valores que aportó o heredó */
    @Column(columnDefinition = "TEXT")
    private String valores;

    /** Aprendizajes clave de vida */
    @Column(columnDefinition = "TEXT")
    private String aprendizajes;

    /** Errores o traumas superados */
    @Column(name = "errores_superados", columnDefinition = "TEXT")
    private String erroresSuperados;

    /** Tradiciones que inició o preservó */
    @Column(columnDefinition = "TEXT")
    private String tradiciones;

    /** Logros y misiones familiares cumplidas */
    @Column(name = "misiones_cumplidas", columnDefinition = "TEXT")
    private String misionesCumplidas;

    /** Legado específico que dejó o dejará */
    @Column(name = "legado_personal", columnDefinition = "TEXT")
    private String legadoPersonal;

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    @Column(name = "position_x")
    private Float positionX;

    @Column(name = "position_y")
    private Float positionY;

    @Column(name = "family_member_id")
    private Long familyMemberId;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<LineageEvent> events = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() { this.createdAt = LocalDateTime.now(); }

    public String getFullName() {
        if (firstName == null && lastName == null) return "Desconocido";
        if (firstName == null) return lastName;
        if (lastName == null) return firstName;
        return firstName + " " + lastName;
    }

    public String getCalculatedAge() {
        if ("future".equals(status) || "projected".equals(generationType)) return "No aplica";
        if ("unknown".equals(status) && birthYear == null) return "No determinada";
        if ("alive".equals(status)) {
            if (birthDate != null) {
                int age = LocalDate.now().getYear() - birthDate.getYear();
                return age + " años";
            }
            if (birthYear != null) {
                int age = LocalDate.now().getYear() - birthYear;
                return (Boolean.TRUE.equals(birthYearApproximate) ? "~" : "") + age + " años";
            }
            return "No determinada";
        }
        if ("deceased".equals(status)) {
            if (birthYear != null && deathYear != null) {
                return (deathYear - birthYear) + " años";
            }
        }
        return "No determinada";
    }

    /** Tipo semántico calculado desde el nivel de generación si no está explícito */
    public String resolvedGenerationType() {
        if (generationType != null) return generationType;
        if (generation == null) return "responsible";
        if (generation <= -2) return "founding";
        if (generation == -1) return "builder";
        if (generation == 0)  return "responsible";
        if (generation == 1)  return "current";
        return "future";
    }
}
