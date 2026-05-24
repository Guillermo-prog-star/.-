package com.integrityfamily.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import java.time.LocalDateTime;

@Entity
@Table(name = "critical_days")
@Filter(name = "familyFilter", condition = "family_id = :familyId")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CriticalDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "family_id", nullable = false)
    private Long familyId;

    @Column(name = "member_id")
    private Long memberId;

    @Column(nullable = false)
    private String category;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "main_emotion")
    private String emotion;

    @Column(columnDefinition = "TEXT")
    private String aiContainmentGuide;

    @Column(name = "day_date", nullable = false)
    private java.time.LocalDate dayDate;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.dayDate == null) {
            this.dayDate = java.time.LocalDate.now();
        }
    }
}
