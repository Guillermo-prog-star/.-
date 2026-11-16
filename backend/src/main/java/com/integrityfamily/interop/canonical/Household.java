package com.integrityfamily.interop.canonical;

import lombok.Builder;

import java.util.List;

/**
 * Equivalente canónico de una familia (mapea desde
 * {@code com.integrityfamily.domain.Family}). Traduce eventualmente a FHIR Group.
 */
@Builder
public record Household(
        String canonicalId,
        String name,
        List<Person> members,
        String municipality,
        String departmentCode,
        String countryCode
) {}
