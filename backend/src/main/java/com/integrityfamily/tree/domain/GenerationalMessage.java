package com.integrityfamily.tree.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "generational_messages")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class GenerationalMessage {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "from_family_id", nullable = false)
    private Long fromFamilyId;

    /** Null = para todas las generaciones futuras */
    @Column(name = "to_family_id")
    private Long toFamilyId;

    @Column(name = "author_name", nullable = false, length = 150)
    private String authorName;

    @Column(length = 200)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "message_type", nullable = false, length = 40)
    @Builder.Default
    private String messageType = "LETTER"; // LETTER | WISDOM | WARNING | BLESSING

    @Builder.Default
    private Boolean sealed = true;

    @Column(name = "open_in_year")
    private Integer openInYear;

    @Column(name = "opened_at")
    private LocalDateTime openedAt;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (sealed == null) sealed = true;
        if (messageType == null) messageType = "LETTER";
    }

    public boolean isReadableNow() {
        if (!Boolean.TRUE.equals(sealed)) return true;
        if (openInYear == null) return true;
        return openInYear <= java.time.LocalDate.now().getYear();
    }
}
