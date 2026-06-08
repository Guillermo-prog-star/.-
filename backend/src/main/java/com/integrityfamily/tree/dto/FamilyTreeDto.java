package com.integrityfamily.tree.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Nodo del árbol generacional. Contiene la familia y sus descendientes directos. */
public record FamilyTreeDto(
    Long familyId,
    String familyName,
    String familyCode,
    LocalDateTime createdAt,
    int generation,          // 0 = raíz ancestral, 1 = hijos, 2 = nietos…
    int memberCount,
    long evidenceCount,
    long gratitudeCount,
    String dnaValores,       // primeros valores del ADN (texto corto)
    String linkedByMember,
    LocalDateTime linkedAt,
    List<FamilyTreeDto> children
) {}
