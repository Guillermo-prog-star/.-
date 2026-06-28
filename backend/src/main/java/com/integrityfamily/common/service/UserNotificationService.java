package com.integrityfamily.common.service;

import com.integrityfamily.common.domain.NotificationLog;
import com.integrityfamily.common.repository.NotificationLogRepository;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.FamilyMember;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserNotificationService {

    private final NotificationLogRepository notificationLogRepository;
    private final WhatsAppService whatsAppService;

    @Value("${whatsapp.notifications.enabled:true}")
    private boolean whatsappEnabled;

    @Value("${whatsapp.notifications.priority-types:RISK_CRITICAL_ALERT,TRAJECTORY_RELAPSED,TRAJECTORY_RELAPSE_REMINDER,TRAJECTORY_SUGGESTION_URGENT,SENTINEL_ALERT,CRISIS_ALERT,PLAN_ASSIGNED}")
    private String priorityTypesRaw;

    @Transactional
    public void push(Family family, FamilyMember member, String type, String title, String message) {
        // 1. Guardar notificación in-app
        try {
            NotificationLog n = new NotificationLog();
            n.setFamily(family);
            n.setFamilyMember(member);
            n.setType(type);
            n.setTitle(title);
            n.setMessage(message);
            if (member != null) {
                n.setRecipientName(member.getFullName());
                n.setRecipientRole(member.getRole());
            }
            notificationLogRepository.save(n);
            log.debug("[NOTIFY] {} → familia={} tipo={}", title, family.getId(), type);
        } catch (Exception e) {
            log.warn("[NOTIFY] Error guardando notificación tipo={}: {}", type, e.getMessage());
        }

        // 2. WhatsApp para tipos de alta prioridad
        if (whatsappEnabled && isPriorityType(type)) {
            sendWhatsApp(family, member, title, message);
        }
    }

    // ── WhatsApp dispatch ──────────────────────────────────────────────────────

    private void sendWhatsApp(Family family, FamilyMember member, String title, String message) {
        try {
            String waMessage = formatWhatsAppMessage(title, message);

            if (member != null) {
                // Notificación personal → al miembro específico
                whatsAppService.sendPersonalizedMessage(member, "ALERT", waMessage);
            } else {
                // Notificación familiar → al número WhatsApp del guardián
                String phone = family.getWhatsapp();
                if (phone != null && !phone.isBlank()) {
                    whatsAppService.sendMessage(phone, waMessage);
                    log.info("[NOTIFY-WA] Enviado a familia {} — tipo deducido de título: {}", family.getId(), title);
                } else {
                    log.debug("[NOTIFY-WA] Familia {} sin número WhatsApp configurado — omitiendo envío.", family.getId());
                }
            }
        } catch (Exception e) {
            log.warn("[NOTIFY-WA] Error enviando WhatsApp para familia {}: {}", family.getId(), e.getMessage());
        }
    }

    /**
     * Formatea el mensaje para WhatsApp: más corto, con emoji de alerta al inicio.
     * WhatsApp tiene límite de 4096 caracteres pero mensajes cortos funcionan mejor.
     */
    private String formatWhatsAppMessage(String title, String message) {
        String body = message.length() > 280 ? message.substring(0, 277) + "…" : message;
        return "*" + title + "*\n\n" + body + "\n\n_Integrity Family — Sistema de Transformación Familiar_";
    }

    private Set<String> priorityTypes() {
        return Arrays.stream(priorityTypesRaw.split("[,\\s]+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    private boolean isPriorityType(String type) {
        return priorityTypes().contains(type);
    }
}
