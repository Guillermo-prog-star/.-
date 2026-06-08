package com.integrityfamily.tree.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "family_tree_links")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class FamilyTreeLink {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "parent_family_id", nullable = false)
    private Long parentFamilyId;

    @Column(name = "child_family_id", nullable = false, unique = true)
    private Long childFamilyId;

    @Column(nullable = false, length = 80)
    @Builder.Default
    private String relationship = "descendant";

    @Column(name = "linked_by_member", length = 150)
    private String linkedByMember;

    @Column(name = "linked_at", nullable = false)
    @Builder.Default
    private LocalDateTime linkedAt = LocalDateTime.now();

    @Column(columnDefinition = "TEXT")
    private String note;

    @PrePersist
    public void prePersist() {
        if (linkedAt == null) linkedAt = LocalDateTime.now();
        if (relationship == null) relationship = "descendant";
    }
}
