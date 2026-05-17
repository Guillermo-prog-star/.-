package com.integrityfamily.security;

import com.integrityfamily.domain.User;
import com.integrityfamily.domain.repository.UserRepository;
import com.integrityfamily.domain.repository.MemberRepository;
import com.integrityfamily.domain.FamilyMember;
import com.integrityfamily.domain.Evaluation;
import com.integrityfamily.domain.repository.EvaluationRepository;
import com.integrityfamily.domain.RiskSnapshot;
import com.integrityfamily.domain.repository.RiskSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * SDD: Evaluador de Seguridad Multitenancy Familiar.
 * Garantiza de forma estricta y determinista que ningún usuario (excepto el Administrador Clínico)
 * pueda leer o escribir información de dinámicas familiares fuera de su propio nodo de pertenencia.
 */
@Slf4j
@Component("familySecurity")
@RequiredArgsConstructor
public class FamilySecurityEvaluator {

    private final UserRepository userRepository;
    private final MemberRepository memberRepository;
    private final EvaluationRepository evaluationRepository;
    private final RiskSnapshotRepository riskSnapshotRepository;

    /**
     * Valida si el usuario actualmente autenticado tiene permisos para interactuar con la familia dada.
     * @param familyId El ID de la familia objetivo.
     * @return true si tiene permisos, false de lo contrario.
     */
    public boolean check(Long familyId) {
        if (familyId == null) return false;

        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            log.warn("⚠️ [SECURITY-DENIED] Intento de acceso sin autenticación.");
            return false;
        }

        String email = auth.getName();
        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null) {
            log.warn("⚠️ [SECURITY-DENIED] Usuario no encontrado en base de datos para email: {}", email);
            return false;
        }

        // El Rol ADMIN representa al Terapeuta/Administrador del sistema.
        // Se le permite el acceso completo a los nodos de familia con propósitos de diagnóstico clínico.
        if (user.getRoles().stream().anyMatch(r -> "ROLE_ADMIN".equals(r.getName()))) {
            log.info("🛡️ [SECURITY-GRANTED] Acceso clínico autorizado para administrador: {}", email);
            return true;
        }

        // El usuario regular (PADRE, MADRE, HIJO) pertenece a una única familia.
        // Se restringe estrictamente a que su family_id coincida con el familyId solicitado.
        boolean authorized = user.getFamily() != null && user.getFamily().getId().equals(familyId);
        if (authorized) {
            log.debug("🔑 [SECURITY-GRANTED] Acceso autorizado para usuario {} al nodo familiar {}.", email, familyId);
        } else {
            log.error("🚨 [SECURITY-BREACH-WARNING] ¡ALERTA DE BRECHA! El usuario {} intentó acceder a información confidencial de la familia ID: {}", email, familyId);
        }

        return authorized;
    }

    /**
     * Valida si el usuario actualmente autenticado tiene permisos para interactuar con el miembro dado.
     * @param memberId El ID del miembro objetivo.
     * @return true si tiene permisos, false de lo contrario.
     */
    public boolean checkMember(Long memberId) {
        if (memberId == null) return false;

        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;

        String email = auth.getName();
        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null) return false;

        if (user.getRoles().stream().anyMatch(r -> "ROLE_ADMIN".equals(r.getName()))) {
            return true;
        }

        FamilyMember member = memberRepository.findById(memberId).orElse(null);
        if (member == null) {
            log.warn("⚠️ [SECURITY-DENIED] Miembro no encontrado con ID: {}", memberId);
            return false;
        }

        boolean authorized = user.getFamily() != null && user.getFamily().getId().equals(member.getFamily().getId());
        
        if (!authorized) {
            log.error("🚨 [SECURITY-BREACH-WARNING] El usuario {} intentó acceder al miembro ID: {} de otra familia", email, memberId);
        }

        return authorized;
    }

    /**
     * Valida si el usuario actualmente autenticado tiene permisos para interactuar con la evaluación dada.
     * @param evaluationId El ID de la evaluación objetivo.
     * @return true si tiene permisos, false de lo contrario.
     */
    public boolean checkEvaluation(Long evaluationId) {
        if (evaluationId == null) return false;

        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;

        String email = auth.getName();
        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null) return false;

        if (user.getRoles().stream().anyMatch(r -> "ROLE_ADMIN".equals(r.getName()))) {
            return true;
        }

        Evaluation evaluation = evaluationRepository.findById(evaluationId).orElse(null);
        if (evaluation == null) {
            log.warn("⚠️ [SECURITY-DENIED] Evaluación no encontrada con ID: {}", evaluationId);
            return false;
        }

        boolean authorized = user.getFamily() != null && user.getFamily().getId().equals(evaluation.getFamily().getId());
        
        if (!authorized) {
            log.error("🚨 [SECURITY-BREACH-WARNING] El usuario {} intentó acceder a la evaluación ID: {} de otra familia", email, evaluationId);
        }

        return authorized;
    }

    /**
     * Valida si el usuario actualmente autenticado tiene permisos para interactuar con la snapshot de riesgo dada.
     * @param snapshotId El ID de la snapshot de riesgo objetivo.
     * @return true si tiene permisos, false de lo contrario.
     */
    public boolean checkRiskSnapshot(Long snapshotId) {
        if (snapshotId == null) return false;

        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;

        String email = auth.getName();
        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null) return false;

        if (user.getRoles().stream().anyMatch(r -> "ROLE_ADMIN".equals(r.getName()))) {
            return true;
        }

        RiskSnapshot snapshot = riskSnapshotRepository.findById(snapshotId).orElse(null);
        if (snapshot == null) {
            log.warn("⚠️ [SECURITY-DENIED] Snapshot de riesgo no encontrado con ID: {}", snapshotId);
            return false;
        }

        boolean authorized = user.getFamily() != null && user.getFamily().getId().equals(snapshot.getFamily().getId());
        
        if (!authorized) {
            log.error("🚨 [SECURITY-BREACH-WARNING] El usuario {} intentó acceder al snapshot ID: {} de otra familia", email, snapshotId);
        }

        return authorized;
    }
}
