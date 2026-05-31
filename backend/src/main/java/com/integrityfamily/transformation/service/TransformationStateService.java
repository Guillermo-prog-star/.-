package com.integrityfamily.transformation.service;

import com.integrityfamily.transformation.domain.TransformationState;
import com.integrityfamily.transformation.domain.TransformationState.OnboardingStep;
import com.integrityfamily.transformation.domain.TransformationState.Pillar;
import com.integrityfamily.transformation.repository.TransformationStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * TransformationStateService — Orquesta el avance de la familia en el flujo de 36 meses.
 *
 * Reglas de negocio:
 *  - No se puede avanzar de paso sin haber completado el anterior.
 *  - El mes determina automáticamente el pilar activo.
 *  - El porcentaje se calcula sobre 36 meses.
 */
@Service
@RequiredArgsConstructor
public class TransformationStateService {

    private final TransformationStateRepository repo;

    /** Obtiene o crea el estado de transformación de una familia. */
    public TransformationState getOrCreate(Long familyId) {
        return repo.findByFamilyId(familyId)
                .orElseGet(() -> repo.save(
                        TransformationState.builder().familyId(familyId).build()
                ));
    }

    /** Avanza el onboarding al siguiente paso. */
    @Transactional
    public TransformationState advanceOnboarding(Long familyId, OnboardingStep step) {
        TransformationState state = getOrCreate(familyId);
        state.setOnboardingStep(step);
        if (step == OnboardingStep.COMPLETED) {
            state.setCurrentMonth(1);
            state.setCurrentPillar(Pillar.RECONOCIMIENTO);
            state.setMilestoneLabel("M1");
        }
        return repo.save(state);
    }

    /** Avanza el mes activo (1–36) y recalcula pilar y progreso. */
    @Transactional
    public TransformationState advanceMonth(Long familyId, int month) {
        if (month < 1 || month > 36) throw new IllegalArgumentException("Mes inválido: " + month);
        TransformationState state = getOrCreate(familyId);
        state.setCurrentMonth(month);
        state.setMilestoneLabel("M" + month);
        state.setProgressPercent(Math.round(((float) month / 36) * 100));
        state.setCurrentPillar(pillarForMonth(month));
        return repo.save(state);
    }

    /** Actualiza el sprint activo. */
    @Transactional
    public TransformationState setSprint(Long familyId, int sprintNumber) {
        TransformationState state = getOrCreate(familyId);
        state.setCurrentSprintNumber(sprintNumber);
        return repo.save(state);
    }

    /** Establece la misión activa. */
    @Transactional
    public TransformationState setActiveMission(Long familyId, Long missionId) {
        TransformationState state = getOrCreate(familyId);
        state.setActiveMissionId(missionId);
        return repo.save(state);
    }

    private Pillar pillarForMonth(int month) {
        if (month <= 6)  return Pillar.RECONOCIMIENTO;
        if (month <= 18) return Pillar.AMOR;
        return Pillar.ENTREGA;
    }
}
