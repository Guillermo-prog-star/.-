// backend/src/main/java/com/integrityfamily/domain/FailedLoginAttempt.java
package com.integrityfamily.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "failed_login_attempts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FailedLoginAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 180)
    private String email;

    @Column(name = "attempted_at", nullable = false)
    private LocalDateTime attemptedAt;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @PrePersist
    void onCreate() {
        if (attemptedAt == null)
            attemptedAt = LocalDateTime.now();
    }
}


