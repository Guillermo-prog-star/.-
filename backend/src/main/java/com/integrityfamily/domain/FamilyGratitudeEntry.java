package com.integrityfamily.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * SDD: Entidad de Registro de Gratitud Familiar.
 */
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "family_gratitude_entries")
@Filter(name = "familyFilter", condition = "family_id = :familyId")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class FamilyGratitudeEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "family_id", nullable = false)
    private Family family;

    @Column(name = "from_member", nullable = false, length = 150)
    private String fromMember;

    @Column(name = "to_member", nullable = false, length = 150)
    private String toMember;

    @Column(nullable = false, length = 1000)
    private String description;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public FamilyGratitudeEntry(Family family, String fromMember, String toMember, String description) {
        this.family = family;
        this.fromMember = fromMember;
        this.toMember = toMember;
        this.description = description;
        this.createdAt = LocalDateTime.now();
    }
}
