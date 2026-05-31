package com.integrityfamily.weeklyplan.service;

import com.integrityfamily.weeklyplan.domain.WeeklyPlan;
import com.integrityfamily.weeklyplan.domain.WeeklyTask;
import com.integrityfamily.weeklyplan.repository.WeeklyPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WeeklyPlanService {

    private final WeeklyPlanRepository repo;

    public List<WeeklyPlan> getAll(Long familyId) {
        return repo.findByFamilyIdOrderBySprintNumberAscPhaseAsc(familyId);
    }

    /**
     * Guarda el plan de una fase específica (upsert).
     * body esperado:
     *   { sprintNumber, phase, weekStartDate?, tasks: [{description, responsible, when, indicator, done}] }
     */
    @Transactional
    public WeeklyPlan save(Long familyId, Map<String, Object> body) {
        int sprintNumber = ((Number) body.getOrDefault("sprintNumber", 1)).intValue();
        String phase     = String.valueOf(body.getOrDefault("phase", "PREPARE")).toUpperCase();

        WeeklyPlan plan = repo.findByFamilyIdAndSprintNumberAndPhase(familyId, sprintNumber, phase)
                .orElseGet(() -> WeeklyPlan.builder()
                        .familyId(familyId)
                        .sprintNumber(sprintNumber)
                        .phase(phase)
                        .weekStartDate(LocalDate.now())
                        .build());

        if (body.containsKey("weekStartDate") && body.get("weekStartDate") != null) {
            plan.setWeekStartDate(LocalDate.parse(String.valueOf(body.get("weekStartDate"))));
        }

        // Reemplazar tareas
        plan.getTasks().clear();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rawTasks = (List<Map<String, Object>>) body.getOrDefault("tasks", List.of());

        int order = 0;
        for (Map<String, Object> t : rawTasks) {
            WeeklyTask task = WeeklyTask.builder()
                    .weeklyPlan(plan)
                    .description(String.valueOf(t.getOrDefault("description", "")))
                    .responsible(String.valueOf(t.getOrDefault("responsible", "")))
                    .when(String.valueOf(t.getOrDefault("when", "")))
                    .indicator(String.valueOf(t.getOrDefault("indicator", "")))
                    .done(Boolean.TRUE.equals(t.get("done")))
                    .sortOrder(order++)
                    .build();
            plan.getTasks().add(task);
        }

        return repo.save(plan);
    }
}
