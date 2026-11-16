// backend/src/main/java/com/integrityfamily/security/CustomUserDetailsService.java
package com.integrityfamily.security;

import com.integrityfamily.domain.Role;
import com.integrityfamily.domain.User;
import com.integrityfamily.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User u = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
        return new org.springframework.security.core.userdetails.User(
                u.getEmail(),
                u.getPasswordHash(),
                u.isEnabled(),
                true, true,
                !u.isCurrentlyLocked(),
                // Bug real preexistente (encontrado 2026-07-18, no introducido por ADR-006):
                // u.getRole() devuelve los roles unidos por coma en un solo string
                // ("ROLE_USER,ROLE_THERAPIST"), y antes se envolvia TODO ese string
                // en una unica SimpleGrantedAuthority. hasRole()/hasAnyRole() hacen
                // match exacto contra cada authority -- ese string compuesto nunca
                // coincide con "ROLE_THERAPIST" ni con ningun rol individual, asi
                // que CUALQUIER usuario con mas de un rol quedaba bloqueado de todo
                // endpoint con @PreAuthorize, sin importar cual de sus roles aplicara.
                // Se corrige generando una authority real por cada Role.
                u.getRoles().stream()
                        .map(Role::getName)
                        .map(SimpleGrantedAuthority::new)
                        .toList());
    }
}


