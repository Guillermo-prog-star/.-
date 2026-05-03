// backend/src/main/java/com/integrityfamily/auth/service/EmailService.java
package com.integrityfamily.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    /** Stub: en Fase 3 se reemplaza por SMTP/SES. */
    public void sendPasswordResetEmail(String email, String rawToken) {
        log.info("[email-stub] Password reset for {} -> token={}", email, rawToken);
    }

    public void sendInvitation(String email, String name, String familyName, String familyCode) {
        log.info("[email-stub] Invitation sent to {} ({}) to join family '{}' [Code: {}]", 
                 email, name, familyName, familyCode);
    }
}



