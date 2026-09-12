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

    public void sendEcosystemInvitation(String toEmail, String participantName, String familyName, String objective) {
        sendEcosystemInvitation(toEmail, participantName, familyName, objective, null);
    }

    public void sendEcosystemInvitation(String toEmail, String participantName, String familyName, String objective, String tempPassword) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Invitación a unirse al Ecosistema de Apoyo - Integrity Family");
            
            StringBuilder text = new StringBuilder();
            text.append("Hola ").append(participantName).append(",\n\n")
                .append("La familia \"").append(familyName).append("\" te ha invitado a formar parte de su Ecosistema de Apoyo en Integrity Family.\n\n")
                .append("Objetivo de la relación: ").append(objective != null ? objective : "Acompañamiento familiar").append("\n\n")
                .append("Para ver y gestionar los datos a los que tienes acceso, por favor inicia sesión en nuestra plataforma:\n")
                .append(frontendUrl).append("/auth/login\n\n");

            if (tempPassword != null && !tempPassword.isEmpty()) {
                text.append("Se ha creado una cuenta profesional automáticamente para ti.\n")
                    .append("Tus credenciales de acceso son:\n")
                    .append("Usuario: ").append(toEmail).append("\n")
                    .append("Contraseña temporal: ").append(tempPassword).append("\n\n")
                    .append("Te recomendamos cambiar tu contraseña una vez que ingreses.\n\n");
            }

            text.append("Saludos,\n")
                .append("El equipo de Integrity Family");

            message.setText(text.toString());
            
            mailSender.send(message);
            log.info("[EmailService] Correo de invitación al ecosistema enviado a: {}", toEmail);
        } catch (Exception e) {
            log.error("[EmailService] Error enviando correo de invitación al ecosistema a: {}", toEmail, e);
        }
    }
}
