package com.integrityfamily.auth.service;

import com.integrityfamily.domain.AuditEvent;
import com.integrityfamily.domain.AuditEventType;
import com.integrityfamily.domain.repository.AuditEventRepository;
import jakarta.servlet.http.HttpServletRequest;
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
@DisplayName("AuditService")
class AuditServiceTest {

    @Mock AuditEventRepository auditEventRepository;
    @InjectMocks AuditService auditService;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private HttpServletRequest mockRequest(String xForwardedFor, String remoteAddr) {
        HttpServletRequest req = mock(HttpServletRequest.class);
        lenient().when(req.getHeader("X-Forwarded-For")).thenReturn(xForwardedFor);
        lenient().when(req.getRemoteAddr()).thenReturn(remoteAddr);
        lenient().when(req.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        return req;
    }

    // ── Extracción de IP ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("extracción de IP")
    class IpExtraction {

        @Test
        @DisplayName("usa el primer valor de X-Forwarded-For cuando es único")
        void xForwardedFor_single_usesItAsIp() {
            HttpServletRequest req = mockRequest("203.0.113.10", "10.0.0.1");
            when(auditEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            auditService.register("user@test.com", AuditEventType.LOGIN_SUCCESS, req, "{}");

            ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(auditEventRepository).save(captor.capture());
            assertThat(captor.getValue().getIpAddress()).isEqualTo("203.0.113.10");
        }

        @Test
        @DisplayName("usa solo el primer IP cuando X-Forwarded-For contiene varios")
        void xForwardedFor_multiple_usesFirst() {
            HttpServletRequest req = mockRequest("203.0.113.10, 198.51.100.5, 10.0.0.2", "10.0.0.1");
            when(auditEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            auditService.register("user@test.com", AuditEventType.LOGIN_SUCCESS, req, "{}");

            ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(auditEventRepository).save(captor.capture());
            assertThat(captor.getValue().getIpAddress()).isEqualTo("203.0.113.10");
        }

        @Test
        @DisplayName("cae a remoteAddr cuando X-Forwarded-For es null")
        void noXForwardedFor_usesRemoteAddr() {
            HttpServletRequest req = mockRequest(null, "192.168.1.50");
            when(auditEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            auditService.register("user@test.com", AuditEventType.LOGOUT, req, null);

            ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(auditEventRepository).save(captor.capture());
            assertThat(captor.getValue().getIpAddress()).isEqualTo("192.168.1.50");
        }

        @Test
        @DisplayName("cae a remoteAddr cuando X-Forwarded-For es blank")
        void blankXForwardedFor_usesRemoteAddr() {
            HttpServletRequest req = mockRequest("   ", "172.16.0.99");
            when(auditEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            auditService.register("user@test.com", AuditEventType.EVALUATION_STARTED, req, "{}");

            ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(auditEventRepository).save(captor.capture());
            assertThat(captor.getValue().getIpAddress()).isEqualTo("172.16.0.99");
        }
    }

    // ── Campos persistidos ────────────────────────────────────────────────────

    @Nested
    @DisplayName("campos del evento guardado")
    class SavedFields {

        @Test
        @DisplayName("persiste actorEmail, eventType, userAgent y metadataJson correctamente")
        void register_savesAllFields() {
            HttpServletRequest req = mockRequest("10.10.10.10", "127.0.0.1");
            when(auditEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            auditService.register("admin@integrity.ia", AuditEventType.FAMILY_REGISTERED, req, "{\"familyId\":42}");

            ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(auditEventRepository).save(captor.capture());
            AuditEvent saved = captor.getValue();
            assertThat(saved.getActorEmail()).isEqualTo("admin@integrity.ia");
            assertThat(saved.getEventType()).isEqualTo(AuditEventType.FAMILY_REGISTERED);
            assertThat(saved.getUserAgent()).isEqualTo("Mozilla/5.0");
            assertThat(saved.getMetadataJson()).isEqualTo("{\"familyId\":42}");
        }
    }
}
