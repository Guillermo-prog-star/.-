package com.integrityfamily.interop.fhir.mapper;

import com.integrityfamily.domain.AuditEventType;
import org.hl7.fhir.r4.model.AuditEvent;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Reference;

import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;
import java.util.Set;

/**
 * {@code domain.AuditEvent} → FHIR {@code AuditEvent}. Puramente mecánico —
 * a diferencia del Terminology Service, aquí no hay ambigüedad clínica que
 * resolver: es un log de actividad del sistema, no un diagnóstico.
 */
public final class AuditEventFhirMapper {

    private static final String TYPE_SYSTEM = "https://integrityfamily.com/fhir/CodeSystem/audit-event-type";
    private static final Set<AuditEventType> FAILURE_OUTCOMES = Set.of(AuditEventType.LOGIN_FAILED, AuditEventType.ACCOUNT_LOCKED);

    private AuditEventFhirMapper() {}

    public static AuditEvent toFhir(com.integrityfamily.domain.AuditEvent source) {
        AuditEvent event = new AuditEvent();
        event.setId("audit-" + source.getId());
        event.setType(new Coding().setSystem(TYPE_SYSTEM).setCode(source.getEventType().name()));
        mapAction(source.getEventType()).ifPresent(event::setAction);

        if (source.getOccurredAt() != null) {
            event.setRecorded(Date.from(source.getOccurredAt().atZone(ZoneId.systemDefault()).toInstant()));
        }
        event.setOutcome(FAILURE_OUTCOMES.contains(source.getEventType())
                ? AuditEvent.AuditEventOutcome._4
                : AuditEvent.AuditEventOutcome._0);

        AuditEvent.AuditEventAgentComponent agent = event.addAgent().setRequestor(true);
        if (source.getActorEmail() != null) {
            agent.setWho(new Reference().setDisplay(source.getActorEmail()));
        }
        if (source.getIpAddress() != null) {
            agent.getNetwork().setAddress(source.getIpAddress());
        }

        event.setSource(new AuditEvent.AuditEventSourceComponent()
                .setSite("Integrity Family")
                .setObserver(new Reference().setDisplay("Integrity Family Backend")));

        if (source.getMetadataJson() != null) {
            event.addEntity().setDescription(source.getMetadataJson());
        }

        return event;
    }

    private static Optional<AuditEvent.AuditEventAction> mapAction(AuditEventType type) {
        return Optional.ofNullable(switch (type) {
            case CONSENT_GRANTED, FAMILY_REGISTERED, EVALUATION_SUBMITTED, EVIDENCE_CREATED -> AuditEvent.AuditEventAction.C;
            case CONSENT_REVOKED -> AuditEvent.AuditEventAction.D;
            case PLAN_TASK_TOGGLED, MISSION_COMPLETED, SPRINT_CLOSED, DAILY_CREATED -> AuditEvent.AuditEventAction.U;
            case LOGIN_SUCCESS, LOGIN_FAILED, LOGOUT, CLI_COMMAND_EXECUTED, FAMILY_HOME_ACTION_EXECUTED -> AuditEvent.AuditEventAction.E;
            default -> null;
        });
    }
}
