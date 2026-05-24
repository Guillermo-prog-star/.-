package com.integrityfamily.common.service;

import com.integrityfamily.common.repository.NotificationLogRepository;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.FamilyMember;
import com.integrityfamily.common.domain.NotificationLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.util.Map;

@Slf4j
@Service
public class WhatsAppService {

    private final NotificationLogRepository notificationLogRepository;
    private final RestClient restClient;

    @Value("${whatsapp.api.url:https://graph.facebook.com/v17.0/12345/messages}")
    private String apiUrl;

    @Value("${whatsapp.api.token:MOCK_TOKEN}")
    private String apiToken;

    public WhatsAppService(NotificationLogRepository notificationLogRepository) {
        this.restClient = RestClient.builder().build();
        this.notificationLogRepository = notificationLogRepository;
    }

    /**
     * [FIX SDD] Requerido por AutomatedReportingService:[70]
     * Envía un reporte o alerta al contacto principal de la familia.
     */
    public void sendToFamily(Family family, String message) {
        String phoneNumber = family.getWhatsapp();
        log.info("📢 [WHATSAPP-FAMILY] Despachando reporte a Familia: {} al número: {}", family.getId(), phoneNumber);

        NotificationLog nl = new NotificationLog();
        nl.setFamily(family);
        nl.setRecipientName("Contacto Principal: " + family.getName());
        nl.setPhoneNumber(phoneNumber);
        nl.setMessage(message);
        nl.setType("FAMILY_REPORT");

        notificationLogRepository.save(nl);
        this.sendMessage(phoneNumber, message);
    }

    public void sendPersonalizedMessage(FamilyMember FamilyMember, String type, String context) {
        String role = FamilyMember.getRole();
        String name = FamilyMember.getFullName();
        // Fallback Multi-Tenant: Usar teléfono del miembro si existe, sino el general de la familia
        String phoneNumber = FamilyMember.getPhone();
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            phoneNumber = FamilyMember.getFamily().getWhatsapp();
        }

        String copy = generateRoleBasedCopy(name, role, type, context);
        log.info("📱 [WHATSAPP-MENTOR] Para: {} ({}) >> {}", name, role, copy);

        NotificationLog nl = new NotificationLog();
        nl.setFamily(FamilyMember.getFamily());
        nl.setRecipientName(name);
        nl.setRecipientRole(role);
        nl.setPhoneNumber(phoneNumber);
        nl.setMessage(copy);
        nl.setType(type);

        notificationLogRepository.save(nl);
        this.sendMessage(phoneNumber, copy);
    }

    /**
     * SDD: Envío de invitación de acceso al sistema para nuevos miembros.
     */
    public void sendInvitation(String phoneNumber, String name, String familyName, String familyCode) {
        String message = String.format(
            "Hola %s, ¡bienvenido a Integrity Family! 🌟 Has sido invitado a unirte al núcleo familiar '%s'. " +
            "Para activar tu cuenta, ingresa aquí: https://integrity.family/auth/register y usa el código: %s",
            name, familyName, familyCode
        );
        log.info("📱 [WHATSAPP-INVITE] Enviando invitación a: {} >> {}", phoneNumber, message);
        this.sendMessage(phoneNumber, message);
    }


    private String generateRoleBasedCopy(String name, String role, String type, String context) {
        String base = "Hola " + name + ", ";
        boolean isLeader = "ADMIN".equalsIgnoreCase(role) || "SENTINEL".equalsIgnoreCase(role);

        return isLeader ? switch (type) {
            case "CRISIS_ALERT" -> base + "⚠️ Sentinel detectó tensión. Tu calma lidera el hogar: " + context;
            case "PLAN_ASSIGNED" -> base + "🎯 Nuevo plan de acción activado: " + context;
            default -> base + "Consejo de integridad: " + context;
        } : switch (type) {
            case "CRISIS_ALERT" -> base + "🛡️ Mantengamos la armonía. ¡Tu actitud suma!";
            case "PLAN_ASSIGNED" -> base + "🚀 ¡Nueva misión familiar!: " + context;
            default -> base + "Pequeño reto de hoy para ti.";
        };
    }

    public void sendMessage(String phoneNumber, String message) {
        if ("MOCK_TOKEN".equals(apiToken) || phoneNumber == null) {
            log.info("📱 [SIMULACIÓN/LOG] +{} >> {}", phoneNumber, message);
            return;
        }
        try {
            restClient.post()
                    .uri(apiUrl)
                    .header("Authorization", "Bearer " + apiToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "messaging_product", "whatsapp",
                            "to", phoneNumber,
                            "type", "text",
                            "text", Map.of("body", message)))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.error("❌ [WHATSAPP-ERROR] {}", e.getMessage());
        }
    }
}
