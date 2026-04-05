package com.integrityfamily.evaluation.domain;
import com.integrityfamily.family.domain.Family;
import com.integrityfamily.family.domain.Member;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime; import java.util.ArrayList; import java.util.List;
@Entity @Table(name="evaluations") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Evaluation {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="family_id",nullable=false) private Family family;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="member_id") private Member member;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private EvaluationStatus status;
    @Column(name="started_at") private LocalDateTime startedAt;
    @Column(name="finalized_at") private LocalDateTime finalizedAt;
    @OneToMany(mappedBy="evaluation",cascade=CascadeType.ALL,orphanRemoval=true)
    @Builder.Default private List<EvaluationAnswer> answers = new ArrayList<>();
    @PrePersist public void pre() {
        if (startedAt==null) startedAt=LocalDateTime.now();
        if (status==null) status=EvaluationStatus.STARTED;
    }
}
