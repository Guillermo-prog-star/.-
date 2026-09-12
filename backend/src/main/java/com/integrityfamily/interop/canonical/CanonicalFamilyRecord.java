package com.integrityfamily.interop.canonical;

import lombok.Builder;

import java.util.List;

/**
 * Raíz del agregado canónico de una familia — todo lo que Integrity Family
 * podría llegar a intercambiar con el ecosistema de salud, en un solo árbol
 * de objetos independiente de FHIR. Los mappers de fases posteriores
 * (Integrity → Canonical, y luego Canonical → FHIR) trabajan a partir de esta
 * clase; el dominio (Family, Evaluation, ImprovementPlan...) no la conoce.
 */
@Builder
public record CanonicalFamilyRecord(
        String canonicalId,
        Household household,
        List<Assessment> assessments,
        List<Risk> risks,
        List<Intervention> interventions,
        List<Outcome> outcomes,
        List<ProfessionalNote> professionalNotes,
        List<Evidence> evidences,
        List<Consent> consents
) {}
