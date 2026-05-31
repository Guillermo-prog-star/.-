package com.integrityfamily.legado.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "family_values")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FamilyValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "family_id", nullable = false)
    private Long familyId;

    @Column(length = 10)
    private String icon;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private int sortOrder;
}
