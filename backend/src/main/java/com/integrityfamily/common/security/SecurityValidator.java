package com.integrityfamily.common.security;

import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.FamilyMember;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.repository.MemberRepository;
import com.integrityfamily.common.exception.NotFoundException;
import com.integrityfamily.domain.User;
import com.integrityfamily.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.security.Principal;

/**
 * SDD-SEC-03: Centralized Nodal Security Validator.
 * Reusable logic to ensure users only access their own family data.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SecurityValidator {

    private final FamilyRepository familyRepository;
    private final MemberRepository memberRepository;
    private final UserRepository userRepository;

    public void validateFamilyOwnership(Long familyId, Principal principal) {
        if (principal == null) {
            throw new AccessDeniedException("No autenticado");
        }
        
        String userEmail = principal.getName();
        
        // 0. Verificar si es Administrador Clínico (Bypass)
        User user = userRepository.findByEmailIgnoreCase(userEmail).orElse(null);
        if (user != null && user.getRoles().stream().anyMatch(r -> "ROLE_ADMIN".equals(r.getName()))) {
            return;
        }

        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new NotFoundException("Familia no encontrada"));
        
        // 1. Verificar si es el Creador (LÃƒÂ­der del Nodo)
        if (family.getCreatedBy() != null && family.getCreatedBy().getEmail().equals(userEmail)) {
            return;
        }

        // 2. Verificar si es un Miembro registrado
        FamilyMember FamilyMember = memberRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AccessDeniedException("No tienes permisos sobre este núcleo familiar"));

        if (!FamilyMember.getFamily().getId().equals(familyId)) {
            log.error("Ã°Å¸Å¡Â¨ [SECURITY-VIOLATION] {} intentÃƒÂ³ acceder a familia {}", userEmail, familyId);
            throw new AccessDeniedException("Acceso denegado");
        }

        if (!FamilyMember.isActive()) {
            throw new AccessDeniedException("Cuenta inactiva");
        }
    }
}


