package com.integrityfamily.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "evaluation_dimension_scores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluationDimensionScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluation_id", nullable = false)
    private Evaluation evaluation;

    @Column(name = "dimension_name", nullable = false, length = 100)
    private String dimensionName;

    @Column(nullable = false)
    private Double score;
}
