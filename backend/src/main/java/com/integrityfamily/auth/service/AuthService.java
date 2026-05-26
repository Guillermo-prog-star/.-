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
import com.integrityfamily.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;
import com.integrityfamily.domain.PasswordResetToken;

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
    private final EmailService emailService;

    @Transactional
    public LoginResponse login(LoginRequest request, String ip, String ua) {
        log.info("[AUTH] Intento de login para: {}", request.email());
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
            User user = userRepository.findByEmail(request.email())
                    .orElseThrow(() -> new BusinessException("Usuario no encontrado", "USER_NOT_FOUND", HttpStatus.NOT_FOUND));

            // [MULTI-TENANT] Cada usuario pertenece a su propia familia (o es ROLE_ADMIN global)
            // No hay restricción por código de familia — el aislamiento lo garantiza FamilySecurityEvaluator
            log.info("[AUTH] Login exitoso para {} — familia: {}", request.email(),
                    user.getFamily() != null ? user.getFamily().getFamilyCode() : "ADMIN-GLOBAL");

            String token = jwtTokenProvider.generate(user);
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());
            com.integrityfamily.auth.dto.UserResponse userDto = com.integrityfamily.auth.dto.UserResponse.from(user);

            return new com.integrityfamily.auth.dto.LoginResponse(token, refreshToken.getToken(), 3600000L, userDto);
        } catch (Exception e) {
            log.error("[AUTH] Error de autenticación para " + request.email(), e);
            accountLockService.registerFailure(request.email());
            if (e instanceof BusinessException) {
                throw e;
            }
            throw new BusinessException("Credenciales inválidas", "INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED);
        }
    }

    @Transactional
    public LoginResponse register(RegisterRequest request, String ip, String ua) {
        log.info("[AUTH] Registro de usuario individual para: {}", request.email());

        if (userRepository.findByEmailIgnoreCase(request.email()).isPresent()) {
            throw new BusinessException("El correo ya está registrado en el sistema.", "EMAIL_ALREADY_USED", HttpStatus.CONFLICT);
        }

        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new BusinessException("Rol base no configurado", "ROLE_NOT_FOUND", HttpStatus.INTERNAL_SERVER_ERROR));

        User newUser = User.builder()
                .email(request.email())
                .fullName(request.fullName())
                .passwordHash(passwordEncoder.encode(request.password()))
                .enabled(true)
                .roles(new ArrayList<>(Collections.singletonList(userRole)))
                .build();
        User saved = userRepository.save(newUser);

        String token = jwtTokenProvider.generate(saved);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(saved.getId());
        com.integrityfamily.auth.dto.UserResponse userDto = com.integrityfamily.auth.dto.UserResponse.from(saved);

        log.info("[AUTH] Usuario registrado exitosamente: {}", request.email());
        return new com.integrityfamily.auth.dto.LoginResponse(token, refreshToken.getToken(), 3600000L, userDto);
    }

    @Transactional
    public LoginResponse registerFamily(RegisterFamilyRequest request, String ip, String ua) {
        log.info("[AUTH] Registro de nueva familia: {} — Admin: {}", request.familyName(), request.email());

        if (userRepository.findByEmailIgnoreCase(request.email()).isPresent()) {
            throw new BusinessException("El correo ya está registrado en el sistema.", "EMAIL_ALREADY_USED", HttpStatus.CONFLICT);
        }

        // Generar código de familia único: IF-{AÑO}-{UUID-corto}
        String year = String.valueOf(java.time.Year.now().getValue());
        String uniqueSuffix = java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String familyCode = "IF-" + year + "-" + uniqueSuffix;

        // 1. Crear la familia
        Family newFamily = Family.builder()
                .name(request.familyName())
                .familyCode(familyCode)
                .currentMilestone("W1")
                .sentinelActive(true)
                .municipio(request.municipio())
                .countryCode(request.countryCode())
                .departmentCode(request.departmentCode())
                .build();

        // 2. Crear el rol FAMILY_ADMIN y el usuario administrador de la familia
        Role familyAdminRole = roleRepository.findByName("ROLE_FAMILY_ADMIN")
                .orElseThrow(() -> new BusinessException("Rol familiar no configurado", "ROLE_NOT_FOUND", HttpStatus.INTERNAL_SERVER_ERROR));

        User adminUser = User.builder()
                .email(request.email())
                .fullName(request.fullName())
                .passwordHash(passwordEncoder.encode(request.password()))
                .enabled(true)
                .roles(new ArrayList<>(Collections.singletonList(familyAdminRole)))
                .build();

        // 3. Persistir en orden correcto para evitar TransientObjectException:
        //    a) Guardar usuario sin familia (para que tenga ID)
        //    b) Guardar familia sin createdBy (para que tenga ID)
        //    c) Vincular ambos y actualizar
        User savedUser = userRepository.save(adminUser);

        newFamily.setCreatedBy(savedUser);
        Family savedFamily = familyRepository.save(newFamily);

        // d) Asociar usuario a su familia y actualizar
        savedUser.setFamily(savedFamily);
        userRepository.save(savedUser);

        String token = jwtTokenProvider.generate(savedUser);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(savedUser.getId());
        com.integrityfamily.auth.dto.UserResponse userDto = com.integrityfamily.auth.dto.UserResponse.from(savedUser);

        log.info("[AUTH] Familia '{}' registrada exitosamente con código: {} — Admin: {}",
                request.familyName(), familyCode, request.email());
        return new com.integrityfamily.auth.dto.LoginResponse(token, refreshToken.getToken(), 3600000L, userDto);
    }

    @Transactional(readOnly = true)
    public UserResponse me(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado", "USER_NOT_FOUND", HttpStatus.NOT_FOUND));
        return UserResponse.from(user);
    }

    @Transactional
    public void logout(String email, String ip, String ua) {
        log.info("[AUTH] Logout para: {}", email);
        userRepository.findByEmail(email).ifPresent(user -> refreshTokenService.deleteByUserId(user.getId()));
    }

    @Transactional
    public void requestPasswordReset(String email, String ip, String ua) {
        log.info("[AUTH] Solicitud de recuperación de contraseña para: {}", email);

        // Seguridad: no revelar si el email existe o no en el sistema
        userRepository.findByEmail(email).ifPresent(user -> {
            // Invalidar tokens previos del usuario para evitar acumulación
            passwordResetTokenRepository.deleteByUser(user);

            // Generar token UUID y su hash SHA-256 para almacenamiento seguro
            String rawToken = UUID.randomUUID().toString();
            String tokenHash = hashToken(rawToken);

            PasswordResetToken prt = PasswordResetToken.builder()
                    .user(user)
                    .tokenHash(tokenHash)
                    .expiresAt(LocalDateTime.now().plusMinutes(30))
                    .build();
            passwordResetTokenRepository.save(prt);

            emailService.sendPasswordResetEmail(email, rawToken);
            log.info("[AUTH] Token de recuperación generado y enviado para: {}", email);
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request, String ip, String ua) {
        log.info("[AUTH] Ejecutando cambio de contraseña via token de recuperación...");

        String tokenHash = hashToken(request.token());

        PasswordResetToken prt = passwordResetTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BusinessException("Token inválido o expirado", "INVALID_RESET_TOKEN", HttpStatus.BAD_REQUEST));

        if (!prt.isValid()) {
            throw new BusinessException("El enlace de recuperación ha expirado o ya fue utilizado", "EXPIRED_RESET_TOKEN", HttpStatus.BAD_REQUEST);
        }

        User user = prt.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        prt.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(prt);

        log.info("[AUTH] Contraseña actualizada exitosamente para: {}", user.getEmail());
    }

    /** Genera el hash SHA-256 de un token raw para almacenamiento seguro. */
    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible en este entorno", e);
        }
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
