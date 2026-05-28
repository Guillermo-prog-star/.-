package com.integrityfamily.guardian.service;

import com.integrityfamily.ai.provider.AiProvider;
import com.integrityfamily.ai.service.PromptGenerator;
import com.integrityfamily.common.exception.BusinessException;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.FamilyMember;
import com.integrityfamily.domain.ImprovementPlan;
import com.integrityfamily.domain.PlanTask;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.repository.ImprovementPlanRepository;
import com.integrityfamily.guardian.dto.GuardianBriefingResponse;
import com.integrityfamily.participation.service.ParticipationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class GuardianBriefingService {

    private final FamilyRepository familyRepository;
    private final ImprovementPlanRepository planRepository;
    private final ParticipationService participationService;
    private final PromptGenerator promptGenerator;
    private final AiProvider aiProvider;

    @Transactional(readOnly = true)
    public GuardianBriefingResponse getBriefing(Long familyId) {
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new BusinessException("Familia no encontrada", "FAMILY_NOT_FOUND", HttpStatus.NOT_FOUND));

        Long guardianId = family.getGuardianMemberId();
        String guardianName = family.getMembers().stream()
                .filter(m -> m.getId().equals(guardianId))
                .findFirst()
                .map(FamilyMember::getFullName)
                .orElse("Guardián");

        // Participación real de los últimos 7 días
        ParticipationService.FamilyParticipationSummary participation =
                participationService.getSummary(familyId, guardianId);

        // Construir lista de miembros para el response
        List<GuardianBriefingResponse.MemberSummary> memberSummaries = participation.activities().stream()
                .map(a -> new GuardianBriefingResponse.MemberSummary(
                        a.memberId(), a.fullName(), a.activeThisWeek(), a.daysSinceLastActivity()))
                .toList();

        // Miembros inactivos (sin actividad en los últimos 7 días)
        List<String> inactiveNames = participation.activities().stream()
                .filter(a -> !a.activeThisWeek() && !a.memberId().equals(guardianId))
                .map(ParticipationService.MemberActivity::fullName)
                .toList();

        // El miembro más inactivo (mayor días sin actividad)
        ParticipationService.MemberActivity mostInactive = participation.activities().stream()
                .filter(a -> !a.activeThisWeek() && !a.memberId().equals(guardianId))
                .max(Comparator.comparingLong(ParticipationService.MemberActivity::daysSinceLastActivity))
                .orElse(null);

        // Plan activo: hito y tasa de completado
        List<ImprovementPlan> plans = planRepository.findByFamilyId(familyId);
        String currentMilestone = family.getCurrentMilestone();
        double completionRate = 0.0;
        if (!plans.isEmpty()) {
            ImprovementPlan latest = plans.get(plans.size() - 1);
            List<PlanTask> tasks = latest.getTasks();
            long total = tasks.size();
            long completed = tasks.stream().filter(PlanTask::isCompleted).count();
            completionRate = total > 0 ? (double) completed / total : 0.0;
        }

        // Generar mensaje IA
        String aiMessage = generateAiMessage(
                guardianName,
                participation.fatigueSignal(),
                participation.activeParticipants(),
                participation.inactiveMembers(),
                inactiveNames,
                mostInactive,
                currentMilestone,
                completionRate
        );

        log.info("[BRIEFING] Guardián {} — activos={}, inactivos={}, fatiga={}",
                guardianName, participation.activeParticipants(), participation.inactiveMembers(), participation.fatigueSignal());

        return new GuardianBriefingResponse(
                guardianName,
                participation.fatigueSignal(),
                participation.activeParticipants(),
                participation.inactiveMembers(),
                memberSummaries,
                currentMilestone,
                completionRate,
                aiMessage
        );
    }

    private String generateAiMessage(
            String guardianName,
            String fatigueSignal,
            int activeCount,
            int inactiveCount,
            List<String> inactiveNames,
            ParticipationService.MemberActivity mostInactive,
            String currentMilestone,
            double completionRate
    ) {
        try {
            String prompt = promptGenerator.buildGuardianBriefingPrompt(
                    guardianName,
                    fatigueSignal,
                    activeCount,
                    inactiveCount,
                    inactiveNames,
                    mostInactive != null ? mostInactive.fullName() : null,
                    mostInactive != null ? mostInactive.daysSinceLastActivity() : 0,
                    currentMilestone,
                    completionRate
            );
            return aiProvider.generateWithFullPrompt(prompt);
        } catch (Exception e) {
            log.warn("[BRIEFING] No se pudo generar mensaje IA para el Guardián: {}", e.getMessage());
            return inactiveCount > 0
                    ? "Esta semana " + inactiveCount + " miembro(s) no han participado. Cuando tengas un momento, una pequeña invitación sin presión puede marcar la diferencia."
                    : "Tu familia está activa esta semana. Sigue adelante.";
        }
    }

    /**
     * Genera un mensaje de re-invitación personalizado para un miembro inactivo.
     * El Guardián puede copiar y enviar el texto resultante por WhatsApp o mensaje directo.
     */
    @Transactional(readOnly = true)
    public String generateReengagementMessage(Long familyId, Long targetMemberId) {
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new BusinessException("Familia no encontrada", "FAMILY_NOT_FOUND", HttpStatus.NOT_FOUND));

        Long guardianId = family.getGuardianMemberId();
        String guardianName = family.getMembers().stream()
                .filter(m -> m.getId().equals(guardianId))
                .findFirst()
                .map(FamilyMember::getFullName)
                .orElse("el Guardián");

        String targetName = family.getMembers().stream()
                .filter(m -> m.getId().equals(targetMemberId))
                .findFirst()
                .map(FamilyMember::getFullName)
                .orElseThrow(() -> new BusinessException("Miembro no encontrado", "MEMBER_NOT_FOUND", HttpStatus.NOT_FOUND));

        ParticipationService.FamilyParticipationSummary summary = participationService.getSummary(familyId, guardianId);
        long daysSince = summary.activities().stream()
                .filter(a -> a.memberId().equals(targetMemberId))
                .findFirst()
                .map(ParticipationService.MemberActivity::daysSinceLastActivity)
                .orElse(7L);

        log.info("[REENGAGE] Guardián {} generando mensaje para {} ({} días inactivo)", guardianName, targetName, daysSince);

        String prompt = promptGenerator.buildReengagementPrompt(guardianName, targetName, daysSince, family.getName());
        try {
            return aiProvider.generateWithFullPrompt(prompt);
        } catch (Exception e) {
            log.warn("[REENGAGE] Error generando mensaje: {}", e.getMessage());
            return "Hola " + targetName + ", te echamos de menos esta semana. ¿Te apetece unirte a la próxima actividad familiar? 🙂";
        }
    }
}
