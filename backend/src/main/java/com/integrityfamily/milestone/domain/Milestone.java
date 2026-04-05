package com.integrityfamily.milestone.domain;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="milestones") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Milestone {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="milestone_key",nullable=false,unique=true,length=20) private String milestoneKey;
    @Column(nullable=false,length=50) private String label;
    @Column(nullable=false) private Integer months;
    @Column(nullable=false,length=80) private String phase;
    @Column(nullable=false,length=30) private String bloque;
    @Column(name="sort_order",nullable=false) private Integer sortOrder;
}
