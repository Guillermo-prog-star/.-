package com.integrityfamily.familyhome.adapter;

import com.integrityfamily.domain.FamilySprint;
import com.integrityfamily.domain.SprintMission;
import com.integrityfamily.domain.repository.FamilySprintRepository;
import com.integrityfamily.domain.repository.SprintMissionRepository;
import com.integrityfamily.dto.home.SprintDisplayStatus;
import com.integrityfamily.familyhome.port.FamilyInterventionQueryPort;
import com.integrityfamily.familyhome.port.SprintSummarySnapshot;
import com.integrityfamily.familyhome.security.FamilyIdentifierBridge;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class FamilyInterventionQueryPortAdapter implements FamilyInterventionQueryPort {

    private final FamilySprintRepository familySprintRepository;
    private final SprintMissionRepository sprintMissionRepository;
    private final FamilyIdentifierBridge idBridge;

    public FamilyInterventionQueryPortAdapter(
            FamilySprintRepository familySprintRepository,
            SprintMissionRepository sprintMissionRepository,
            FamilyIdentifierBridge idBridge) {
        this.familySprintRepository = familySprintRepository;
        this.sprintMissionRepository = sprintMissionRepository;
        this.idBridge = idBridge;
    }

    @Override
    public SprintSummarySnapshot getActiveSprint(UUID familyId) {
        Optional<Long> fId = idBridge.resolveFamilyId(familyId);
        if (fId.isEmpty()) {
            return null;
        }

        FamilySprint sprint = familySprintRepository.findActiveSprintForFamily(fId.get()).orElse(null);
        if (sprint == null) {
            return null;
        }

        List<SprintMission> missions = sprintMissionRepository.findBySprintId(sprint.getId());
        int total = missions.size();
        int completed = (int) missions.stream().filter(m -> m.getCompletedAt() != null).count();
        SprintMission todayMission = missions.stream()
                .filter(m -> !"COMPLETED".equals(m.getStatus()))
                .findFirst()
                .orElse(null);

        return new SprintSummarySnapshot(
                UUID.nameUUIDFromBytes(("sprint-" + sprint.getId()).getBytes(StandardCharsets.UTF_8)),
                SprintDisplayStatus.ACTIVE,
                sprint.getObjective(),
                completed,
                total,
                todayMission != null ? "mission-" + todayMission.getId() : null,
                todayMission != null ? todayMission.getDescription() : null,
                todayMission != null ? "/sprint/missions/" + todayMission.getId() : null
        );
    }
}
