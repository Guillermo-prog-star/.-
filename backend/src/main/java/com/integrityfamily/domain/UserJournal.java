package com.integrityfamily.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Entidad de Bitácora Privada (UserJournal) para la Capa Personal.
 * Permite a cada miembro registrar emociones, reflexiones y aprendizajes privados.
 */
@Entity
@Table(name = "user_journals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserJournal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(length = 50)
    private String emotionalState; // Ej: Ansioso, Feliz, Frustrado

    @Column(length = 50)
    private String category; // Ej: Aprendizaje, Conflicto, Logro

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
