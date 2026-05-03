package com.integrityfamily.domain;

// Sincronización de dominio centralizado
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "family_members")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FamilyMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Builder.Default
    @OneToMany(mappedBy = "responsible", cascade = CascadeType.ALL)
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
    private Family family;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id")
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


