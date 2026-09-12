package com.integrityfamily.interop.fhir.service;

import com.integrityfamily.common.exception.BusinessException;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.repository.AuditEventRepository;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.repository.MemberRepository;
import com.integrityfamily.interop.fhir.mapper.AuditEventFhirMapper;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.AuditEvent;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Rastro de auditoría de una familia, envuelto como recursos FHIR
 * {@code AuditEvent}. {@code domain.AuditEvent} no tiene {@code family_id}
 * — solo {@code actorEmail} — así que se resuelve el conjunto de emails
 * relevantes (dueño de la familia, miembros, y el email sintético
 * {@code family_<id>@integrityfamily.com} que usan los eventos a nivel de
 * sistema, ver {@code SprintService}/{@code TaskEvidenceService}) y se
 * reutiliza la consulta existente de {@code AuditEventRepository} — no se
 * agrega una columna nueva ni un índice nuevo para esto.
 */
@Service
@RequiredArgsConstructor
public class AuditFhirTrailService {

    private final FamilyRepository familyRepository;
    private final MemberRepository memberRepository;
    private final AuditEventRepository auditEventRepository;

    @Transactional(readOnly = true)
    public List<AuditEvent> getFamilyAuditTrail(Long familyId) {
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new BusinessException("Familia no encontrada.", "INTEROP_FAMILY_NOT_FOUND", HttpStatus.NOT_FOUND));

        List<String> emails = new ArrayList<>();
        if (family.getCreatedBy() != null && family.getCreatedBy().getEmail() != null) {
            emails.add(family.getCreatedBy().getEmail());
        }
        memberRepository.findByFamilyId(familyId).stream()
                .map(com.integrityfamily.domain.FamilyMember::getEmail)
                .filter(Objects::nonNull)
                .forEach(emails::add);
        emails.add("family_" + familyId + "@integrityfamily.com");

        return auditEventRepository.findByActorEmailInOrderByOccurredAtDesc(emails)
                .stream().map(AuditEventFhirMapper::toFhir).toList();
    }

    public Bundle assembleBundle(Long familyId) {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);
        bundle.setId("family-" + familyId + "-audit-trail");
        for (AuditEvent event : getFamilyAuditTrail(familyId)) {
            bundle.addEntry().setFullUrl("AuditEvent/" + event.getId()).setResource(event);
        }
        return bundle;
    }
}
