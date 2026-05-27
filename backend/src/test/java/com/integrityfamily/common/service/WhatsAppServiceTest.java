package com.integrityfamily.common.service;

import com.integrityfamily.common.domain.NotificationLog;
import com.integrityfamily.common.repository.NotificationLogRepository;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.FamilyMember;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Estrategia: espía la instancia real para corto-circuitar sendMessage()
 * (que llama a RestClient y requiere @Value inyectado por Spring).
 * Los tests verifican la lógica de routing de teléfono y generación de copia.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WhatsAppService")
class WhatsAppServiceTest {

    @Mock NotificationLogRepository notificationLogRepository;

    WhatsAppService service;

    @BeforeEach
    void setUp() {
        service = spy(new WhatsAppService(notificationLogRepository));
        // Corto-circuitar la llamada HTTP para todos los tests
        doNothing().when(service).sendMessage(any(), any());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Family family(String whatsapp) {
        return Family.builder().id(1L).name("Los García").whatsapp(whatsapp).build();
    }

    private FamilyMember member(String role, String phone, Family family) {
        return FamilyMember.builder()
                .id(10L).fullName("Juan García").role(role)
                .phone(phone).family(family)
                .build();
    }

    // ── sendToFamily ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("sendToFamily")
    class SendToFamily {

        @Test
        @DisplayName("guarda NotificationLog con tipo FAMILY_REPORT y delega sendMessage")
        void savesLogAndDelegatesMessage() {
            Family fam = family("+573001234567");
            when(notificationLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.sendToFamily(fam, "Mensaje de prueba");

            ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
            verify(notificationLogRepository).save(captor.capture());
            NotificationLog saved = captor.getValue();
            assertThat(saved.getType()).isEqualTo("FAMILY_REPORT");
            assertThat(saved.getPhoneNumber()).isEqualTo("+573001234567");
            assertThat(saved.getRecipientName()).contains("Los García");
            verify(service).sendMessage("+573001234567", "Mensaje de prueba");
        }
    }

    // ── sendPersonalizedMessage — routing de teléfono ─────────────────────────

    @Nested
    @DisplayName("routing de telefono en sendPersonalizedMessage")
    class PhoneRouting {

        @Test
        @DisplayName("usa el telefono del miembro cuando esta disponible")
        void memberHasPhone_usesMemberPhone() {
            Family fam = family("+57888");
            FamilyMember m = member("ADMIN", "+57999", fam);
            when(notificationLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.sendPersonalizedMessage(m, "CRISIS_ALERT", "Contexto");

            verify(service).sendMessage(eq("+57999"), any());
        }

        @Test
        @DisplayName("cae a whatsapp de la familia cuando el telefono del miembro es null")
        void memberPhoneNull_fallsBackToFamilyWhatsapp() {
            Family fam = family("+57777");
            FamilyMember m = member("ADMIN", null, fam);
            when(notificationLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.sendPersonalizedMessage(m, "PLAN_ASSIGNED", "Plan");

            verify(service).sendMessage(eq("+57777"), any());
        }

        @Test
        @DisplayName("cae a whatsapp de la familia cuando el telefono del miembro es string vacio")
        void memberPhoneEmpty_fallsBackToFamilyWhatsapp() {
            Family fam = family("+57666");
            FamilyMember m = member("SENTINEL", "   ", fam);
            when(notificationLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.sendPersonalizedMessage(m, "CRISIS_ALERT", "Contexto");

            verify(service).sendMessage(eq("+57666"), any());
        }

        @Test
        @DisplayName("guarda NotificationLog con campos correctos")
        void savesNotificationLogWithCorrectFields() {
            Family fam = family("+57555");
            FamilyMember m = member("PADRE", "+57555", fam);
            when(notificationLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.sendPersonalizedMessage(m, "PLAN_ASSIGNED", "Misión X");

            ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
            verify(notificationLogRepository).save(captor.capture());
            NotificationLog saved = captor.getValue();
            assertThat(saved.getRecipientName()).isEqualTo("Juan García");
            assertThat(saved.getRecipientRole()).isEqualTo("PADRE");
            assertThat(saved.getType()).isEqualTo("PLAN_ASSIGNED");
        }
    }

    // ── generateRoleBasedCopy (vía sendPersonalizedMessage) ───────────────────

    @Nested
    @DisplayName("generacion de copia por rol")
    class RoleBasedCopy {

        private void assertMessageContains(String role, String type, String context, String expected) {
            Family fam = family(null);
            FamilyMember m = member(role, null, fam);
            when(notificationLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.sendPersonalizedMessage(m, type, context);

            ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
            verify(notificationLogRepository).save(captor.capture());
            assertThat(captor.getValue().getMessage()).contains(expected);
        }

        @Test @DisplayName("lider ADMIN + CRISIS_ALERT → mensaje de sentinela con contexto")
        void admin_crisisAlert_sentinelMessage() {
            assertMessageContains("ADMIN", "CRISIS_ALERT", "tension", "Sentinel");
        }

        @Test @DisplayName("lider SENTINEL + PLAN_ASSIGNED → mensaje de plan de accion")
        void sentinel_planAssigned_planMessage() {
            assertMessageContains("SENTINEL", "PLAN_ASSIGNED", "plan X", "plan de acción");
        }

        @Test @DisplayName("miembro no lider + CRISIS_ALERT → mensaje de armonia")
        void member_crisisAlert_harmonyMessage() {
            assertMessageContains("PADRE", "CRISIS_ALERT", "", "armonía");
        }

        @Test @DisplayName("miembro no lider + PLAN_ASSIGNED → mensaje de mision")
        void member_planAssigned_missionMessage() {
            assertMessageContains("HIJO", "PLAN_ASSIGNED", "reto", "misión familiar");
        }

        @Test @DisplayName("lider + tipo desconocido → consejo de integridad")
        void admin_unknownType_consejoMessage() {
            assertMessageContains("ADMIN", "OTRO", "", "Consejo de integridad");
        }

        @Test @DisplayName("miembro + tipo desconocido → pequeño reto de hoy")
        void member_unknownType_smallChallengeMessage() {
            assertMessageContains("MADRE", "OTRO", "", "Pequeño reto");
        }
    }
}
