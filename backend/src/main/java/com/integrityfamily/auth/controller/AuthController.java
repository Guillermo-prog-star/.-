package com.integrityfamily.auth.controller;

import com.integrityfamily.auth.dto.*;
import com.integrityfamily.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return authService.login(request, getClientIp(httpRequest), getUserAgent(httpRequest));
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public LoginResponse register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        return authService.register(request, getClientIp(httpRequest), getUserAgent(httpRequest));
    }

    @PostMapping("/register-family")
    @ResponseStatus(HttpStatus.CREATED)
    public LoginResponse registerFamily(@Valid @RequestBody RegisterFamilyRequest request, HttpServletRequest httpRequest) {
        return authService.registerFamily(request, getClientIp(httpRequest), getUserAgent(httpRequest));
    }


    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest request, HttpServletRequest httpRequest) {
        authService.requestPasswordReset(request.email(), getClientIp(httpRequest), getUserAgent(httpRequest));
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.OK)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request, HttpServletRequest httpRequest) {
        authService.resetPassword(request, getClientIp(httpRequest), getUserAgent(httpRequest));
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        // En JwtAuthenticationFilter validamos el estado real contra BD.
        // El Authentication.getName() contiene el email seguro validado.
        return authService.me(authentication.getName());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(Authentication authentication, HttpServletRequest httpRequest) {
        authService.logout(authentication.getName(), getClientIp(httpRequest), getUserAgent(httpRequest));
    }

    // --- Helpers de ExtracciÃƒÂ³n de Contexto ---
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


