package com.integrityfamily.weeklyplan.service;

import com.integrityfamily.weeklyplan.domain.WeeklyPlan;
import com.integrityfamily.weeklyplan.repository.WeeklyPlanRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WeeklyPlanService")
class WeeklyPlanServiceTest {

    @Mock WeeklyPlanRepository repo;
    @InjectMocks WeeklyPlanService service;

    private static final long FAM_ID = 1L;

    // ── getAll ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getAll")
    class GetAll {

        @Test
        @DisplayName("delega al repositorio ordenado por sprint y fase")
        void delegatesToRepo() {
            List<WeeklyPlan> plans = List.of(WeeklyPlan.builder().familyId(FAM_ID).build());
            when(repo.findByFamilyIdOrderBySprintNumberAscPhaseAsc(FAM_ID)).thenReturn(plans);

            List<WeeklyPlan> result = service.getAll(FAM_ID);

            assertThat(result).isSameAs(plans);
        }
    }

    // ── save ──────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("body vacío → crea plan con sprintNumber=1 y phase='PREPARE'")
        void emptyBody_createsWithDefaults() {
            when(repo.findByFamilyIdAndSprintNumberAndPhase(FAM_ID, 1, "PREPARE"))
                    .thenReturn(Optional.empty());
            when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            WeeklyPlan result = service.save(FAM_ID, Map.of());

            assertThat(result.getSprintNumber()).isEqualTo(1);
            assertThat(result.getPhase()).isEqualTo("PREPARE");
            assertThat(result.getFamilyId()).isEqualTo(FAM_ID);
        }

        @Test
        @DisplayName("sprintNumber=3, phase='execute' → busca 'EXECUTE' en repo")
        void phaseIsUppercasedBeforeRepoLookup() {
            when(repo.findByFamilyIdAndSprintNumberAndPhase(FAM_ID, 3, "EXECUTE"))
                    .thenReturn(Optional.empty());
            when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            WeeklyPlan result = service.save(FAM_ID, Map.of("sprintNumber", 3, "phase", "execute"));

            assertThat(result.getPhase()).isEqualTo("EXECUTE");
            assertThat(result.getSprintNumber()).isEqualTo(3);
        }

        @Test
        @DisplayName("plan existente → misma entidad reutilizada y guardada")
        void existingPlan_reusesSameEntity() {
            WeeklyPlan existing = WeeklyPlan.builder().id(99L).familyId(FAM_ID)
                    .sprintNumber(1).phase("PREPARE").build();
            when(repo.findByFamilyIdAndSprintNumberAndPhase(FAM_ID, 1, "PREPARE"))
                    .thenReturn(Optional.of(existing));
            when(repo.save(existing)).thenReturn(existing);

            WeeklyPlan result = service.save(FAM_ID, Map.of());

            assertThat(result.getId()).isEqualTo(99L);
            verify(repo).save(existing);
        }

        @Test
        @DisplayName("weekStartDate en body → se parsea y asigna al plan")
        void weekStartDate_parsed() {
            when(repo.findByFamilyIdAndSprintNumberAndPhase(FAM_ID, 1, "PREPARE"))
                    .thenReturn(Optional.empty());
            when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            WeeklyPlan result = service.save(FAM_ID, Map.of("weekStartDate", "2025-09-01"));

            assertThat(result.getWeekStartDate()).isEqualTo(LocalDate.of(2025, 9, 1));
        }

        @Test
        @DisplayName("2 tareas → sortOrder 0 y 1; tasks añadidas al plan")
        void tasks_builtWithIncrementalSortOrder() {
            when(repo.findByFamilyIdAndSprintNumberAndPhase(FAM_ID, 1, "PREPARE"))
                    .thenReturn(Optional.empty());
            when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Map<String, Object> body = new HashMap<>();
            body.put("tasks", List.of(
                    Map.of("description", "Tarea A", "responsible", "Papá", "when", "Lunes", "indicator", "OK"),
                    Map.of("description", "Tarea B", "responsible", "Mamá", "when", "Martes", "indicator", "OK")));

            WeeklyPlan result = service.save(FAM_ID, body);

            assertThat(result.getTasks()).hasSize(2);
            assertThat(result.getTasks().get(0).getSortOrder()).isEqualTo(0);
            assertThat(result.getTasks().get(1).getSortOrder()).isEqualTo(1);
            assertThat(result.getTasks().get(0).getDescription()).isEqualTo("Tarea A");
        }

        @Test
        @DisplayName("tarea con done=true → done asignado; sin done → false")
        void taskDoneFlag_trueFalse() {
            when(repo.findByFamilyIdAndSprintNumberAndPhase(FAM_ID, 1, "PREPARE"))
                    .thenReturn(Optional.empty());
            when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Map<String, Object> t1 = new HashMap<>();
            t1.put("description", "Completada");
            t1.put("done", true);

            Map<String, Object> t2 = new HashMap<>();
            t2.put("description", "Pendiente");

            Map<String, Object> body = new HashMap<>();
            body.put("tasks", List.of(t1, t2));

            WeeklyPlan result = service.save(FAM_ID, body);

            assertThat(result.getTasks().get(0).isDone()).isTrue();
            assertThat(result.getTasks().get(1).isDone()).isFalse();
        }

        @Test
        @DisplayName("plan existente con tareas → lista limpiada antes de reconstruirse")
        void existingPlan_tasksClearedBeforeRebuild() {
            WeeklyPlan existing = WeeklyPlan.builder().id(5L).familyId(FAM_ID)
                    .sprintNumber(1).phase("PREPARE").build();
            when(repo.findByFamilyIdAndSprintNumberAndPhase(FAM_ID, 1, "PREPARE"))
                    .thenReturn(Optional.of(existing));
            when(repo.save(existing)).thenReturn(existing);

            // body sin tareas → lista queda vacía
            WeeklyPlan result = service.save(FAM_ID, Map.of());

            assertThat(result.getTasks()).isEmpty();
        }
    }
}
