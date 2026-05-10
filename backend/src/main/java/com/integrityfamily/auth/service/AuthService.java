package com.integrityfamily.auth.service;

import com.integrityfamily.auth.dto.*;
import com.integrityfamily.domain.User;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.Role;
import com.integrityfamily.domain.repository.UserRepository;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.repository.RoleRepository;
import com.integrityfamily.domain.repository.PasswordResetTokenRepository;
import com.integrityfamily.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final FamilyRepository familyRepository;
    private final RoleRepository roleRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final AccountLockService accountLockService;
    private final AuditService auditService;

    @Transactional
    public LoginResponse login(LoginRequest request, String ip, String ua) {
        log.info("[AUTH] Intento de login para: {}", request.email());
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
            User user = userRepository.findByEmail(request.email())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado post-auth"));
            
            String token = jwtTokenProvider.generate(user);
        
        com.integrityfamily.auth.dto.UserResponse userDto = com.integrityfamily.auth.dto.UserResponse.from(user);

        return new com.integrityfamily.auth.dto.LoginResponse(token, 3600000L, userDto);
        } catch (Exception e) {
            log.error("[AUTH] Error de autenticación: {}", e.getMessage());
            accountLockService.registerFailure(request.email());
            throw new RuntimeException("Credenciales inválidas");
        }
    }

    @Transactional
    public LoginResponse register(RegisterRequest request, String ip, String ua) {
        log.info("[AUTH] Registrando nuevo usuario: {}", request.email());

        if (userRepository.existsByEmail(request.email())) {
            throw new RuntimeException("El correo electrónico ya está registrado");
        }

        User user = new User();
        user.setEmail(request.email());
        user.setFullName(request.fullName());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setEnabled(true);

        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_USER").build()));
        
        user.setRoles(Collections.singletonList(userRole));

        User saved = userRepository.save(user);
        log.info("[AUTH] Usuario {} creado con éxito", saved.getEmail());

        String token = jwtTokenProvider.generate(saved);
        UserResponse userDto = UserResponse.from(saved);

        return new LoginResponse(token, 3600000L, userDto);
    }

    @Transactional
    public LoginResponse registerFamily(RegisterFamilyRequest request, String ip, String ua) {
        log.info("[AUTH] Registrando familia: {}", request.familyName());
        
        if (userRepository.existsByEmail(request.email())) {
            throw new RuntimeException("El correo electrónico ya está registrado");
        }

        Family family = new Family();
        family.setName(request.familyName());
        family = familyRepository.save(family);

        User admin = new User();
        admin.setEmail(request.email());
        admin.setFullName(request.fullName());
        admin.setPasswordHash(passwordEncoder.encode(request.password()));
        admin.setFamily(family);
        admin.setEnabled(true);
        
        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_ADMIN").build()));
        
        admin.setRoles(Collections.singletonList(adminRole));
        
        User saved = userRepository.save(admin);
        log.info("[AUTH] Familia {} y Admin {} creados con éxito", family.getId(), saved.getEmail());
        
        String token = jwtTokenProvider.generate(saved);
        UserResponse userDto = UserResponse.from(saved);
        
        return new LoginResponse(token, 3600000L, userDto);
    }


    @Transactional(readOnly = true)
    public UserResponse me(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return UserResponse.from(user);
    }

    public void logout(String email, String ip, String ua) {
        log.info("[AUTH] Logout para: {}", email);
        // Implementar invalidación de token si se usa un TokenStore/Blacklist
    }

    public void requestPasswordReset(String email, String ip, String ua) {
        log.info("[AUTH] Solicitud de recuperación de contraseña para: {}", email);
        // Implementar generación de token y envío de email
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request, String ip, String ua) {
        log.info("[AUTH] Ejecutando recuperación de contraseña para el token: {}", request.token());
        // Implementar validación de token y cambio de password
    }
}
