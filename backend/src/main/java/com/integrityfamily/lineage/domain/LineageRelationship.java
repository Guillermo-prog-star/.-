package com.integrityfamily.lineage.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "lineage_relationships")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class LineageRelationship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lineage_id", nullable = false)
    private FamilyLineage lineage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_member_id", nullable = false)
    private LineageMember fromMember;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_member_id", nullable = false)
    private LineageMember toMember;

    /** biological | adoptive | step | couple */
    @Column(name = "relationship_type", length = 50)
    private String relationshipType;

    @Column(name = "is_couple")
    private Boolean isCouple;
}
