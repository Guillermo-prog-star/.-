# IFA-HUD Threat Model

This document outlines the security boundaries, identified threats, and corresponding mitigations implemented in the Integrity Family Adaptive HUD (IFA-HUD) module.

## 1. Security Boundaries

The IFA-HUD mediates access between two distinct domains:
- **Family Workspace**: Intended for parents, children, and legal guardians. Focuses on supportive, simplified relational indicators.
- **Professional/Clinical Workspace**: Intended for authorized support networks (therapists, researchers). Encompasses granular psychological indices (ICaF), alerts, and notes.

## 2. Identified Threats & Mitigations

### Threat 1: Cross-Domain Privilege Escalation (Elevation of Privilege)
- **Description**: A regular family member attempts to read raw clinical indicators or therapeutic notes by calling professional endpoints.
- **Mitigation**: Strictly validated server-side `HudAuthorizationPolicy` checking context role membership (`ADULT_MEMBER`, `SUPPORT_PERSON`) and explicit permissions before resolving projections. The request type parameter is ignored for authorization.

### Threat 2: Resource Enumeration (Information Disclosure)
- **Description**: An attacker guesses or enumerates random UUIDs representing families to check if they exist or to obtain active status codes.
- **Mitigation**: Unified anti-enumeration exception mapping. Any authorization failure (unauthorized or missing family relation) is handled homogeneously as a `404 NOT_FOUND` status, with identical payload structure, headers, and media types.

### Threat 3: Class Injection & Data Leakage in Serialization (Information Disclosure)
- **Description**: Professional clinical notes or detailed alerts are accidentally mapped to a family payload due to common base DTO types.
- **Mitigation**: Strong type separation via a sealed `AdaptiveHudView` interface permitting distinct contracts (`FamilyHudView` and `ProfessionalHudView`), and runtime assertions on allowed modules using `HudModulePolicy`.

## 3. Residual Risks

- **Timing Analysis (Timing Attack)**: There is a minor potential latency difference between a request for a non-existent family ID (fast path database miss) and an unauthorized active family ID (which requires checking the membership indices). 
- **Mitigation**: Standard API gateways and rate limiters at the edge block high-frequency enumeration scans, making timing-based extraction unfeasible in practice.
