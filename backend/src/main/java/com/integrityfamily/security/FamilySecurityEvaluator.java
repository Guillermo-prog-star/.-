package com.integrityfamily.security;

import com.integrityfamily.domain.User;
import com.integrityfamily.domain.repository.UserRepository;
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
}
