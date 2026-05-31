package com.integrityfamily.common.service;

import com.integrityfamily.common.domain.NotificationLog;
import com.integrityfamily.common.repository.NotificationLogRepository;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.FamilyMember;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserNotificationService {

    private final NotificationLogRepository notificationLogRepository;

    @Transactional
    public void push(Family family, FamilyMember member, String type, String title, String message) {
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
    }
}
