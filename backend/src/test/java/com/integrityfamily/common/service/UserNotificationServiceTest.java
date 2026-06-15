package com.integrityfamily.common.service;

import com.integrityfamily.common.domain.NotificationLog;
import com.integrityfamily.common.repository.NotificationLogRepository;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.FamilyMember;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserNotificationService")
class UserNotificationServiceTest {

    @Mock NotificationLogRepository notificationLogRepository;
    @InjectMocks UserNotificationService service;

    private final Family family = Family.builder().id(1L).name("Familia Test").build();

    private FamilyMember member(String name, String role) {
        return FamilyMember.builder().id(10L).fullName(name).role(role).build();
    }

    // ── push ──────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("push")
    class Push {

        @Test
        @DisplayName("con miembro → guarda con recipientName y recipientRole del miembro")
        void withMember_setsRecipientFields() {
            FamilyMember m = member("María López", "GUARDIAN");
            ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
            when(notificationLogRepository.save(captor.capture())).thenReturn(new NotificationLog());

            service.push(family, m, "ALERT", "Título alerta", "Mensaje de prueba");

            NotificationLog saved = captor.getValue();
            assertThat(saved.getRecipientName()).isEqualTo("María López");
            assertThat(saved.getRecipientRole()).isEqualTo("GUARDIAN");
            assertThat(saved.getType()).isEqualTo("ALERT");
            assertThat(saved.getTitle()).isEqualTo("Título alerta");
            assertThat(saved.getMessage()).isEqualTo("Mensaje de prueba");
        }

        @Test
        @DisplayName("sin miembro (null) → guarda sin recipientName ni recipientRole")
        void withNullMember_noRecipientFields() {
            ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
            when(notificationLogRepository.save(captor.capture())).thenReturn(new NotificationLog());

            service.push(family, null, "INFO", "Info", "Contenido");

            NotificationLog saved = captor.getValue();
            assertThat(saved.getRecipientName()).isNull();
            assertThat(saved.getRecipientRole()).isNull();
            assertThat(saved.getFamily()).isSameAs(family);
        }

        @Test
        @DisplayName("repositorio lanza excepción → error silenciado (try-catch)")
        void repositoryThrows_exceptionSwallowed() {
            when(notificationLogRepository.save(any())).thenThrow(new RuntimeException("BD caída"));

            // No debe lanzar excepción
            service.push(family, null, "WARN", "Error", "Mensaje");

            verify(notificationLogRepository).save(any());
        }
    }
}
