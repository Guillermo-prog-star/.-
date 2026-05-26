package com.integrityfamily.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    /**
     * Envía el enlace de recuperación de contraseña.
     * Fase 2: log estructurado con URL completa.
     * Fase 3: reemplazar por SMTP/SES — agregar spring.mail.* en application.yml
     *         y JavaMailSender / MimeMessage aquí.
     */
    public void sendPasswordResetEmail(String email, String rawToken) {
        String resetUrl = frontendUrl + "/auth/reset-password?token=" + rawToken;

        log.warn("\n" +
                "╔══════════════════════════════════════════════════════════╗\n" +
                "║         IF ·· RECUPERACIÓN DE CONTRASEÑA                ║\n" +
                "╠══════════════════════════════════════════════════════════╣\n" +
                "║  Email  : {}\n" +
                "║  Expira : 30 minutos\n" +
                "║  URL    : {}\n" +
                "╚══════════════════════════════════════════════════════════╝",
                email, resetUrl);
    }

    public void sendInvitation(String email, String name, String familyName, String familyCode) {
        log.info("[email-stub] Invitation sent to {} ({}) to join family '{}' [Code: {}]",
                email, name, familyName, familyCode);
    }
}
