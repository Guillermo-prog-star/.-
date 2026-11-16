package com.integrityfamily.familyhome.adapter;

import com.integrityfamily.domain.Evaluation;
import com.integrityfamily.domain.EvaluationStatus;
import com.integrityfamily.domain.repository.EvaluationRepository;
import com.integrityfamily.domain.repository.FamilySprintRepository;
import com.integrityfamily.domain.repository.SprintMissionRepository;
import com.integrityfamily.dto.home.JourneyStage;
import com.integrityfamily.familyhome.port.FamilyJourneyQueryPort;
import com.integrityfamily.familyhome.port.JourneySnapshot;
import com.integrityfamily.familyhome.security.FamilyIdentifierBridge;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * No existe todavía una máquina de estados real para JourneyStage (V1.2 no la modela).
 * Esta es una heurística conservadora sobre datos reales (evaluaciones + sprints)
 * mientras se define el modelo formal de etapas.
 */
@Component
public class FamilyJourneyQueryPortAdapter implements FamilyJourneyQueryPort {

    private static final List<EvaluationStatus> FINALIZED_STATUSES =
            List.of(EvaluationStatus.FINALIZED, EvaluationStatus.FINISHED, EvaluationStatus.COMPLETED);
    private static final List<EvaluationStatus> IN_PROGRESS_STATUSES =
            List.of(EvaluationStatus.STARTED, EvaluationStatus.IN_PROGRESS);

    private final EvaluationRepository evaluationRepository;
    private final FamilySprintRepository familySprintRepository;
    private final SprintMissionRepository sprintMissionRepository;
    private final FamilyIdentifierBridge idBridge;

    public FamilyJourneyQueryPortAdapter(
            EvaluationRepository evaluationRepository,
            FamilySprintRepository familySprintRepository,
            SprintMissionRepository sprintMissionRepository,
            FamilyIdentifierBridge idBridge) {
        this.evaluationRepository = evaluationRepository;
        this.familySprintRepository = familySprintRepository;
        this.sprintMissionRepository = sprintMissionRepository;
        this.idBridge = idBridge;
    }

    @Override
    public JourneySnapshot getJourneyStatus(UUID familyId) {
        Optional<Long> fId = idBridge.resolveFamilyId(familyId);
        if (fId.isEmpty()) {
            return new JourneySnapshot(JourneyStage.NEW_FAMILY, 0, 0);
        }
        Long id = fId.get();

        List<Evaluation> evaluations = evaluationRepository.findByFamilyId(id);
        boolean hasFinalized = evaluations.stream().anyMatch(e -> FINALIZED_STATUSES.contains(e.getStatus()));
        boolean hasInProgress = evaluations.stream().anyMatch(e -> IN_PROGRESS_STATUSES.contains(e.getStatus()));

        int completedTasks = (int) sprintMissionRepository.countCompletedByFamilyId(id);
        int totalTasks = (int) sprintMissionRepository.countTotalByFamilyId(id);
        boolean hasActiveSprint = familySprintRepository.findActiveSprintForFamily(id).isPresent();

        JourneyStage stage;
        if (hasActiveSprint) {
            stage = JourneyStage.ACTIVE_HOME;
        } else if (hasFinalized) {
            stage = totalTasks > 0 ? JourneyStage.RESUMING_HOME : JourneyStage.FIRST_SPRINT_PENDING;
        } else if (hasInProgress) {
            stage = JourneyStage.ASSESSMENT_IN_PROGRESS;
        } else {
            stage = JourneyStage.NEW_FAMILY;
        }

        return new JourneySnapshot(stage, completedTasks, totalTasks);
    }
}
