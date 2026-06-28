package com.integrityfamily.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    @Value("${spring.mail.username:noreply@integrityfamily.com}")
    private String fromEmail;

    private final JavaMailSender mailSender;

    /**
     * Envía el enlace de recuperación de contraseña.
     * Ahora utiliza JavaMailSender para enviar el correo real (Fase 3).
     */
    public void sendPasswordResetEmail(String email, String rawToken) {
        String resetUrl = frontendUrl + "/auth/reset-password?token=" + rawToken;

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject("Recuperación de Contraseña - Integrity Family");
            message.setText("Hola,\n\n" +
                    "Has solicitado restablecer tu contraseña.\n" +
                    "Por favor, haz clic en el siguiente enlace para crear una nueva (expira en 30 minutos):\n\n" +
                    resetUrl + "\n\n" +
                    "Si no solicitaste este cambio, puedes ignorar este mensaje.\n\n" +
                    "Saludos,\n" +
                    "El equipo de Integrity Family");
            
            mailSender.send(message);
            log.info("[EmailService] Correo de recuperación enviado a: {}", email);
        } catch (Exception e) {
            log.error("[EmailService] Error enviando correo de recuperación a: {}", email, e);
        }
    }

    public void sendInvitation(String email, String name, String familyName, String familyCode) {
        log.info("[email-stub] Invitation sent to {} ({}) to join family '{}' [Code: {}]",
                email, name, familyName, familyCode);
    }
}
