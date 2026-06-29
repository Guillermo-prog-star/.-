package com.integrityfamily.support.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "support_network_members")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class SupportNetworkMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(unique = true, nullable = false)
    private String email;

    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SupportSpecialty specialty;

    @Column(name = "license_number")
    private String licenseNumber;

    @Column(name = "institution_name")
    private String institutionName;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() { this.createdAt = LocalDateTime.now(); }
}
