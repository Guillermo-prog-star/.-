package com.integrityfamily.interop.controller;

import com.integrityfamily.common.dto.ApiResponse;
import com.integrityfamily.interop.canonical.CanonicalFamilyRecord;
import com.integrityfamily.interop.fhir.service.AuditFhirTrailService;
import com.integrityfamily.interop.fhir.service.FhirBundleAssembler;
import com.integrityfamily.interop.fhir.service.FhirSerializationService;
import com.integrityfamily.interop.service.CanonicalFamilyRecordAssembler;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Expone el registro canónico ensamblado (Fase 3), su traducción a FHIR
 * (Fase 4) y el rastro de auditoría en FHIR (Fase 5) — endpoints de solo
 * lectura para verificación manual e inspección; no reemplazan ningún
 * endpoint existente.
 */
@RestController
@RequestMapping("/api/families/{familyId}/interop")
@RequiredArgsConstructor
public class InteropController {

    private final CanonicalFamilyRecordAssembler assembler;
    private final FhirBundleAssembler fhirBundleAssembler;
    private final FhirSerializationService fhirSerializationService;
    private final AuditFhirTrailService auditFhirTrailService;

    @PreAuthorize("@familySecurity.check(#familyId)")
    @GetMapping("/canonical-record")
    public ApiResponse<CanonicalFamilyRecord> getCanonicalRecord(@PathVariable Long familyId) {
        return ApiResponse.ok(assembler.assemble(familyId));
    }

    /** Bundle FHIR R4 (Group + Patient + Observation) en JSON, como lo recibiría una IPS o el Ministerio. */
    @PreAuthorize("@familySecurity.check(#familyId)")
    @GetMapping(value = "/fhir-bundle", produces = "application/fhir+json")
    public String getFhirBundle(@PathVariable Long familyId) {
        CanonicalFamilyRecord record = assembler.assemble(familyId);
        return fhirSerializationService.toJson(fhirBundleAssembler.assemble(record));
    }

    /** Bundle FHIR R4 de recursos AuditEvent — quién accedió/modificó qué y cuándo, en el formato que espera el Ministerio. */
    @PreAuthorize("@familySecurity.check(#familyId)")
    @GetMapping(value = "/fhir-audit-trail", produces = "application/fhir+json")
    public String getFhirAuditTrail(@PathVariable Long familyId) {
        return fhirSerializationService.toJson(auditFhirTrailService.assembleBundle(familyId));
    }
}
