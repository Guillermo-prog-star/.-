package com.integrityfamily.tree.dto;

import jakarta.validation.constraints.NotBlank;

public record LinkRequest(
    @NotBlank String parentFamilyCode, // código de la familia origen (ej: IF-CO-2026-0001)
    String linkedByMember,             // nombre de quien crea el vínculo
    String note                        // descripción opcional
) {}
