package com.integrityfamily.ai.service;

import com.integrityfamily.domain.ImprovementPlan;
import com.integrityfamily.domain.PlanTask;
import com.integrityfamily.domain.repository.ImprovementPlanRepository;
import com.integrityfamily.scanner.domain.FamilyAlert;
import com.integrityfamily.scanner.repository.FamilyAlertRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConversationGoalManager")
class ConversationGoalManagerTest {

    @Mock FamilyAlertRepository     alertRepository;
    @Mock ImprovementPlanRepository planRepository;
    @InjectMocks ConversationGoalManager manager;

    private static final long FAM_ID = 1L;
    private static final long MEM_ID = 10L;

    private FamilyAlert alert(String severity) {
        return FamilyAlert.builder().familyId(FAM_ID).severity(severity).build();
    }

    @Test
    @DisplayName("familyId=null → 'GENERAL' sin consultar repos")
    void nullFamilyId_returnsGeneral() {
        String result = manager.inferGoal(null, MEM_ID);

        assertThat(result).isEqualTo("GENERAL");
        verifyNoInteractions(alertRepository, planRepository);
    }

    @Test
    @DisplayName("alerta CRITICAL → 'CRISIS_CONTAINMENT'")
    void criticalAlert_returnsCrisisContainment() {
        when(alertRepository.findByFamilyIdAndResolvedFalseOrderByCreatedAtDesc(FAM_ID))
                .thenReturn(List.of(alert("CRITICAL")));

        assertThat(manager.inferGoal(FAM_ID, MEM_ID)).isEqualTo("CRISIS_CONTAINMENT");
    }

    @Test
    @DisplayName("alerta HIGH → 'SUPPORT'")
    void highAlert_returnsSupport() {
        when(alertRepository.findByFamilyIdAndResolvedFalseOrderByCreatedAtDesc(FAM_ID))
                .thenReturn(List.of(alert("HIGH")));

        assertThat(manager.inferGoal(FAM_ID, MEM_ID)).isEqualTo("SUPPORT");
    }

    @Test
    @DisplayName("alerta de severidad menor (ej. MEDIUM) → 'SUPPORT'")
    void lowerSeverityAlert_returnsSupport() {
        when(alertRepository.findByFamilyIdAndResolvedFalseOrderByCreatedAtDesc(FAM_ID))
                .thenReturn(List.of(alert("MEDIUM")));

        assertThat(manager.inferGoal(FAM_ID, MEM_ID)).isEqualTo("SUPPORT");
    }

    @Test
    @DisplayName("sin alertas, tiene misiones pendientes → 'PLANNING'")
    void noAlerts_hasPendingMissions_returnsPlanning() {
        PlanTask pending = PlanTask.builder().completed(false).build();
        ImprovementPlan plan = ImprovementPlan.builder().build();
        plan.getTasks().add(pending);
        when(alertRepository.findByFamilyIdAndResolvedFalseOrderByCreatedAtDesc(FAM_ID)).thenReturn(List.of());
        when(planRepository.findByFamilyId(FAM_ID)).thenReturn(List.of(plan));

        assertThat(manager.inferGoal(FAM_ID, MEM_ID)).isEqualTo("PLANNING");
    }

    @Test
    @DisplayName("sin alertas, todas las misiones completadas → 'GENERAL'")
    void noAlerts_allMissionsDone_returnsGeneral() {
        PlanTask done = PlanTask.builder().completed(true).build();
        ImprovementPlan plan = ImprovementPlan.builder().build();
        plan.getTasks().add(done);
        when(alertRepository.findByFamilyIdAndResolvedFalseOrderByCreatedAtDesc(FAM_ID)).thenReturn(List.of());
        when(planRepository.findByFamilyId(FAM_ID)).thenReturn(List.of(plan));

        assertThat(manager.inferGoal(FAM_ID, MEM_ID)).isEqualTo("GENERAL");
    }

    @Test
    @DisplayName("sin alertas ni planes → 'GENERAL'")
    void noAlertsNoPlans_returnsGeneral() {
        when(alertRepository.findByFamilyIdAndResolvedFalseOrderByCreatedAtDesc(FAM_ID)).thenReturn(List.of());
        when(planRepository.findByFamilyId(FAM_ID)).thenReturn(List.of());

        assertThat(manager.inferGoal(FAM_ID, MEM_ID)).isEqualTo("GENERAL");
    }

    @Test
    @DisplayName("repo lanza excepción → falla silenciosamente, retorna 'GENERAL'")
    void repoThrows_silentFailure_returnsGeneral() {
        when(alertRepository.findByFamilyIdAndResolvedFalseOrderByCreatedAtDesc(FAM_ID))
                .thenThrow(new RuntimeException("DB error"));

        assertThat(manager.inferGoal(FAM_ID, MEM_ID)).isEqualTo("GENERAL");
    }
}
