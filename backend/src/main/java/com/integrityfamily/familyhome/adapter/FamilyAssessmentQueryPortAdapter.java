package com.integrityfamily.familyhome.adapter;

import com.integrityfamily.domain.Evaluation;
import com.integrityfamily.domain.EvaluationStatus;
import com.integrityfamily.domain.repository.EvaluationAnswerRepository;
import com.integrityfamily.domain.repository.EvaluationRepository;
import com.integrityfamily.domain.repository.QuestionRepository;
import com.integrityfamily.familyhome.port.AssessmentProgressSnapshot;
import com.integrityfamily.familyhome.port.AssessmentReturnSnapshot;
import com.integrityfamily.familyhome.port.FamilyAssessmentQueryPort;
import com.integrityfamily.familyhome.security.FamilyIdentifierBridge;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class FamilyAssessmentQueryPortAdapter implements FamilyAssessmentQueryPort {

    private static final List<EvaluationStatus> IN_PROGRESS_STATUSES =
            List.of(EvaluationStatus.STARTED, EvaluationStatus.IN_PROGRESS);
    private static final List<EvaluationStatus> FINALIZED_STATUSES =
            List.of(EvaluationStatus.FINALIZED, EvaluationStatus.COMPLETED, EvaluationStatus.FINISHED);

    private final EvaluationRepository evaluationRepository;
    private final EvaluationAnswerRepository evaluationAnswerRepository;
    private final QuestionRepository questionRepository;
    private final FamilyIdentifierBridge idBridge;

    public FamilyAssessmentQueryPortAdapter(
            EvaluationRepository evaluationRepository,
            EvaluationAnswerRepository evaluationAnswerRepository,
            QuestionRepository questionRepository,
            FamilyIdentifierBridge idBridge) {
        this.evaluationRepository = evaluationRepository;
        this.evaluationAnswerRepository = evaluationAnswerRepository;
        this.questionRepository = questionRepository;
        this.idBridge = idBridge;
    }

    @Override
    public AssessmentProgressSnapshot getProgress(UUID familyId) {
        Optional<Long> fId = idBridge.resolveFamilyId(familyId);
        int totalQuestions = questionRepository.findByActiveTrue().size();
        if (fId.isEmpty()) {
            return new AssessmentProgressSnapshot(0, totalQuestions);
        }

        Evaluation current = evaluationRepository.findByFamilyIdOrderByStartedAtDesc(fId.get()).stream()
                .filter(e -> IN_PROGRESS_STATUSES.contains(e.getStatus()))
                .findFirst()
                .orElse(null);

        int completedQuestions = current != null
                ? (int) evaluationAnswerRepository.countByEvaluationId(current.getId())
                : 0;

        return new AssessmentProgressSnapshot(completedQuestions, totalQuestions);
    }

    @Override
    public AssessmentReturnSnapshot getReturn(UUID familyId) {
        Optional<Long> fId = idBridge.resolveFamilyId(familyId);
        if (fId.isEmpty()) {
            return new AssessmentReturnSnapshot(List.of(), null, false);
        }

        Optional<Evaluation> lastFinalized = FINALIZED_STATUSES.stream()
                .map(status -> evaluationRepository.findFirstByFamilyIdAndStatusOrderByFinalizedAtDesc(fId.get(), status))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();

        String priorityOpportunity = lastFinalized.map(Evaluation::getCriticalDimension).orElse(null);
        return new AssessmentReturnSnapshot(List.of(), priorityOpportunity, lastFinalized.isPresent());
    }
}
