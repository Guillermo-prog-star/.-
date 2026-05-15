package com.integrityfamily.auth.service;

import com.integrityfamily.auth.dto.*;
import com.integrityfamily.domain.User;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.RefreshToken;
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
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public LoginResponse login(LoginRequest request, String ip, String ua) {
        log.info("[AUTH] Intento de login para: {}", request.email());
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
            User user = userRepository.findByEmail(request.email())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado post-auth"));
            
            // [SEGURIDAD DE PROTOCOLO] Confidencialidad y aislamiento total de datos del Nodo Armenia
            if (user.getFamily() != null) {
                String code = user.getFamily().getFamilyCode();
                if (code != null && !code.equals("IF-CO-QUI-2026-0004") && !user.getRole().equals("ROLE_ADMIN")) {
                    log.warn("[SECURITY] Acceso bloqueado. Usuario {} pertenece a familia no autorizada: {}", request.email(), code);
                    throw new RuntimeException("Acceso restringido: Esta cuenta no pertenece a la red familiar autorizada.");
                }
            } else if (!user.getRole().equals("ROLE_ADMIN")) {
                log.warn("[SECURITY] Acceso bloqueado. Usuario {} no tiene familia asignada.", request.email());
                throw new RuntimeException("Acceso restringido: Esta cuenta no tiene una red familiar asignada.");
            }

            String token = jwtTokenProvider.generate(user);
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());
            com.integrityfamily.auth.dto.UserResponse userDto = com.integrityfamily.auth.dto.UserResponse.from(user);

            return new com.integrityfamily.auth.dto.LoginResponse(token, refreshToken.getToken(), 3600000L, userDto);
        } catch (Exception e) {
            log.error("[AUTH] Error de autenticación para {}: {}", request.email(), e.getMessage());
            accountLockService.registerFailure(request.email());
            throw new RuntimeException(e.getMessage() != null && e.getMessage().contains("Acceso restringido") ? e.getMessage() : "Credenciales inválidas");
        }
    }

    @Transactional
    public LoginResponse register(RegisterRequest request, String ip, String ua) {
        log.warn("[SECURITY] Intento de autoregistro denegado para {}. El registro de cuentas externas está deshabilitado por seguridad.", request.email());
        throw new RuntimeException("El autoregistro de cuentas externas está restringido temporalmente por políticas de confidencialidad de la red familiar.");
    }

    @Transactional
    public LoginResponse registerFamily(RegisterFamilyRequest request, String ip, String ua) {
        log.warn("[SECURITY] Intento de registro familiar denegado para {}. Registro restringido para la única familia autorizada.", request.familyName());
        throw new RuntimeException("La creación de redes familiares externas está deshabilitada. Únicamente el Nodo Familiar Lopez Rivera está autorizado en esta red.");
    }

    @Transactional(readOnly = true)
    public UserResponse me(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return UserResponse.from(user);
    }

    @Transactional
    public void logout(String email, String ip, String ua) {
        log.info("[AUTH] Logout para: {}", email);
        userRepository.findByEmail(email).ifPresent(user -> refreshTokenService.deleteByUserId(user.getId()));
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

    @Transactional
    public LoginResponse refreshToken(RefreshTokenRequest request, String ip, String ua) {
        log.info("[AUTH] Solicitando refresco de token JWT desde IP: {}", ip);
        RefreshToken refreshToken = refreshTokenService.findByToken(request.refreshToken());
        refreshTokenService.verifyExpiration(refreshToken);

        User user = refreshToken.getUser();
        String token = jwtTokenProvider.generate(user);
        com.integrityfamily.auth.dto.UserResponse userDto = com.integrityfamily.auth.dto.UserResponse.from(user);

        return new LoginResponse(token, refreshToken.getToken(), 3600000L, userDto);
    }
}
