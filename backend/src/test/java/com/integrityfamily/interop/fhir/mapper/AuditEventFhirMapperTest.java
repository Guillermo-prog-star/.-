package com.integrityfamily.interop.fhir.mapper;

import com.integrityfamily.domain.AuditEventType;
import org.hl7.fhir.r4.model.AuditEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AuditEventFhirMapper")
class AuditEventFhirMapperTest {

    @Test
    @DisplayName("evento exitoso → outcome SUCCESS (_0)")
    void shouldMapSuccessOutcome() {
        com.integrityfamily.domain.AuditEvent source = com.integrityfamily.domain.AuditEvent.builder()
                .id(1L).eventType(AuditEventType.CONSENT_GRANTED)
                .actorEmail("ana@test.com").ipAddress("192.168.1.1")
                .metadataJson("{\"consentId\": 5}")
                .occurredAt(LocalDateTime.of(2026, 7, 1, 10, 0))
                .build();

        AuditEvent fhirEvent = AuditEventFhirMapper.toFhir(source);

        assertThat(fhirEvent.getId()).isEqualTo("audit-1");
        assertThat(fhirEvent.getType().getCode()).isEqualTo("CONSENT_GRANTED");
        assertThat(fhirEvent.getAction()).isEqualTo(AuditEvent.AuditEventAction.C);
        assertThat(fhirEvent.getOutcome()).isEqualTo(AuditEvent.AuditEventOutcome._0);
        assertThat(fhirEvent.getAgentFirstRep().getWho().getDisplay()).isEqualTo("ana@test.com");
        assertThat(fhirEvent.getAgentFirstRep().getNetwork().getAddress()).isEqualTo("192.168.1.1");
        assertThat(fhirEvent.getEntityFirstRep().getDescription()).isEqualTo("{\"consentId\": 5}");
        assertThat(fhirEvent.getSource().getSite()).isEqualTo("Integrity Family");
    }

    @Test
    @DisplayName("LOGIN_FAILED → outcome MINORFAILURE (_4)")
    void shouldMapFailureOutcome() {
        com.integrityfamily.domain.AuditEvent source = com.integrityfamily.domain.AuditEvent.builder()
                .id(2L).eventType(AuditEventType.LOGIN_FAILED)
                .occurredAt(LocalDateTime.now()).build();

        AuditEvent fhirEvent = AuditEventFhirMapper.toFhir(source);

        assertThat(fhirEvent.getOutcome()).isEqualTo(AuditEvent.AuditEventOutcome._4);
        assertThat(fhirEvent.getAction()).isEqualTo(AuditEvent.AuditEventAction.E);
    }

    @Test
    @DisplayName("CONSENT_REVOKED → action D")
    void shouldMapDeleteAction() {
        com.integrityfamily.domain.AuditEvent source = com.integrityfamily.domain.AuditEvent.builder()
                .id(3L).eventType(AuditEventType.CONSENT_REVOKED)
                .occurredAt(LocalDateTime.now()).build();

        AuditEvent fhirEvent = AuditEventFhirMapper.toFhir(source);

        assertThat(fhirEvent.getAction()).isEqualTo(AuditEvent.AuditEventAction.D);
    }

    @Test
    @DisplayName("tipo sin acción mapeada (ej. SESSION_EXPIRED) → action queda sin setear, no lanza excepción")
    void shouldLeaveActionUnset_forUnmappedType() {
        com.integrityfamily.domain.AuditEvent source = com.integrityfamily.domain.AuditEvent.builder()
                .id(4L).eventType(AuditEventType.SESSION_EXPIRED)
                .occurredAt(LocalDateTime.now()).build();

        AuditEvent fhirEvent = AuditEventFhirMapper.toFhir(source);

        assertThat(fhirEvent.hasAction()).isFalse();
    }

    @Test
    @DisplayName("sin actorEmail, sin ip, sin metadata → no lanza excepción, campos opcionales vacíos")
    void shouldHandleMinimalEvent() {
        com.integrityfamily.domain.AuditEvent source = com.integrityfamily.domain.AuditEvent.builder()
                .id(5L).eventType(AuditEventType.FAMILY_REGISTERED)
                .occurredAt(LocalDateTime.now()).build();

        AuditEvent fhirEvent = AuditEventFhirMapper.toFhir(source);

        assertThat(fhirEvent.getAgentFirstRep().hasWho()).isFalse();
        assertThat(fhirEvent.hasEntity()).isFalse();
    }
}
