package com.integrityfamily.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * SDD: Entidad de Miembro de Familia.
 * Refactorizada para evitar LazyInitializationException en serialización.
 */
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "family_members")
@Filter(name = "familyFilter", condition = "family_id = :familyId")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class FamilyMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Builder.Default
    @OneToMany(mappedBy = "responsible", cascade = CascadeType.ALL)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private List<PlanTask> tasks = new ArrayList<>();

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "first_name")
    private String firstName;

    @Column(unique = true)
    private String email;

    @Column
    private String password;

    private String phone;

    private String role;

    private Integer age;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "autonomy_level")
    private Integer autonomyLevel;

    @Column(name = "responsibility_level")
    private Integer responsibilityLevel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Family family;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private User user;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @PrePersist
    public void prePersist() {
        if (this.joinedAt == null) {
            this.joinedAt = LocalDateTime.now();
        }
    }
}
