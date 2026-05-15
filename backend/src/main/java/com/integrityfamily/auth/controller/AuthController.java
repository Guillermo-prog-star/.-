package com.integrityfamily.auth.controller;

import com.integrityfamily.auth.dto.*;
import com.integrityfamily.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "1. Identity & Autenticación", description = "Contrato de servicios para la gestión de acceso, tokens JWT, creación de familias y recuperación de credenciales.")
public class AuthController {

    private final AuthService authService;
    private final com.integrityfamily.plan.scheduler.PlanComplianceScheduler planComplianceScheduler;

    @Operation(summary = "Iniciar sesión", description = "Autentica a un usuario mediante correo electrónico y contraseña, devolviendo el token JWT Bearer, el Refresh Token y los datos de la sesión.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Autenticación exitosa", content = @Content(schema = @Schema(implementation = LoginResponse.class))),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
        @ApiResponse(responseCode = "401", description = "Credenciales incorrectas o cuenta inactiva")
    })
    @PostMapping("/login")
    public LoginResponse login(
            @Parameter(description = "Credenciales de acceso", required = true) @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        return authService.login(request, getClientIp(httpRequest), getUserAgent(httpRequest));
    }

    @Operation(summary = "Refrescar token de acceso", description = "Genera un nuevo token JWT Bearer utilizando un Refresh Token válido y no expirado.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Refresco exitoso", content = @Content(schema = @Schema(implementation = LoginResponse.class))),
        @ApiResponse(responseCode = "400", description = "Token de refresco inválido o expirado")
    })
    @PostMapping("/refresh-token")
    public LoginResponse refreshToken(
            @Parameter(description = "Token de refresco", required = true) @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest) {
        return authService.refreshToken(request, getClientIp(httpRequest), getUserAgent(httpRequest));
    }

    @Operation(summary = "Registrar usuario individual", description = "Registra un nuevo usuario en la plataforma. Opcionalmente acepta un código de voucher alfa.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Usuario registrado exitosamente", content = @Content(schema = @Schema(implementation = LoginResponse.class))),
        @ApiResponse(responseCode = "400", description = "Error de validación o correo ya registrado")
    })
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public LoginResponse register(
            @Parameter(description = "Datos del usuario", required = true) @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {
        return authService.register(request, getClientIp(httpRequest), getUserAgent(httpRequest));
    }

    @Operation(summary = "Registrar nuevo núcleo familiar", description = "Crea una nueva familia en el sistema junto con su miembro administrador fundador.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Familia y administrador creados exitosamente", content = @Content(schema = @Schema(implementation = LoginResponse.class))),
        @ApiResponse(responseCode = "400", description = "Las contraseñas no coinciden o la familia ya existe")
    })
    @PostMapping("/register-family")
    @ResponseStatus(HttpStatus.CREATED)
    public LoginResponse registerFamily(
            @Parameter(description = "Datos del núcleo familiar y fundador", required = true) @Valid @RequestBody RegisterFamilyRequest request,
            HttpServletRequest httpRequest) {
        return authService.registerFamily(request, getClientIp(httpRequest), getUserAgent(httpRequest));
    }

    @Operation(summary = "Solicitar recuperación de contraseña", description = "Envía un enlace/token de recuperación al correo electrónico especificado si existe en el sistema.")
    @ApiResponses({
        @ApiResponse(responseCode = "202", description = "Solicitud de recuperación aceptada en proceso"),
        @ApiResponse(responseCode = "400", description = "Correo electrónico inválido")
    })
    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void forgotPassword(
            @Parameter(description = "Correo del usuario", required = true) @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpRequest) {
        authService.requestPasswordReset(request.email(), getClientIp(httpRequest), getUserAgent(httpRequest));
    }

    @Operation(summary = "Restablecer contraseña", description = "Actualiza la contraseña del usuario utilizando un token de recuperación válido.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Contraseña restablecida exitosamente"),
        @ApiResponse(responseCode = "400", description = "Token inválido o expirado")
    })
    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.OK)
    public void resetPassword(
            @Parameter(description = "Datos de restablecimiento", required = true) @Valid @RequestBody ResetPasswordRequest request,
            HttpServletRequest httpRequest) {
        authService.resetPassword(request, getClientIp(httpRequest), getUserAgent(httpRequest));
    }

    @Operation(summary = "Obtener perfil del usuario actual (Me)", description = "Devuelve los datos del usuario autenticado y su núcleo familiar basado en el token JWT.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Perfil recuperado exitosamente", content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(responseCode = "401", description = "No autorizado o token expirado")
    })
    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        return authService.me(authentication.getName());
    }

    @Operation(summary = "Cerrar sesión", description = "Invalida la sesión actual del usuario y elimina los tokens de refresco en persistencia.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Sesión cerrada exitosamente")
    })
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(Authentication authentication, HttpServletRequest httpRequest) {
        authService.logout(authentication.getName(), getClientIp(httpRequest), getUserAgent(httpRequest));
    }

    @Operation(summary = "Disparador manual de recordatorios semanales", description = "Endpoint de auditoría y diagnóstico para emitir alertas de hábitos semanales por WhatsApp.")
    @ApiResponse(responseCode = "200", description = "Disparo completado")
    @GetMapping("/plans/tasks/remind-weekly/trigger")
    public com.integrityfamily.common.dto.ApiResponse<String> triggerWeeklyReminders() {
        planComplianceScheduler.sendWeeklyHabitReminders();
        return com.integrityfamily.common.dto.ApiResponse.ok("Disparo manual de recordatorios de hábitos semanales iniciado.");
    }

    // --- Helpers de Extracción de Contexto ---
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip != null ? ip : "UNKNOWN";
    }

    private String getUserAgent(HttpServletRequest request) {
        String ua = request.getHeader("User-Agent");
        return ua != null ? ua : "UNKNOWN";
    }
}
