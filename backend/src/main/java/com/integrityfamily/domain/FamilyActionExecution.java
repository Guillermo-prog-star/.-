package com.integrityfamily.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Registro de ejecución de un comando semántico del Family Action Engine (IFRM-D Hito 5).
 * La combinación (familyId, action, idempotencyKey) es única: un reintento con la misma
 * clave de idempotencia encuentra el registro existente y no vuelve a ejecutar el comando.
 */
@Entity
@Table(name = "family_action_executions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FamilyActionExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "family_id", nullable = false, length = 36)
    private String familyId;

    @Column(nullable = false, length = 80)
    private String action;

    @Column(name = "idempotency_key", nullable = false, length = 120)
    private String idempotencyKey;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "result_sprint_ref", length = 36)
    private String resultSprintRef;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
