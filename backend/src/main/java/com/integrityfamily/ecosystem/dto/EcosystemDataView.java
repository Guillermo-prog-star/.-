package com.integrityfamily.ecosystem.dto;

import com.integrityfamily.ecosystem.domain.NetworkType;
import lombok.Builder;
import lombok.Data;

/**
 * Vista de datos de una familia filtrada por el scope autorizado del vínculo.
 * Los campos son null cuando el scope no incluye ese permiso.
 * aggregatedOnly=true indica datos anónimos de nivel territorial (sin familyId).
 */
@Data
@Builder
public class EcosystemDataView {

    private Long familyId;           // null para TERRITORIAL
    private NetworkType networkType;
    private int accessLevel;
    private String participantName;

    // ── Datos según scope ─────────────────────────────────────────────────
    private Double icfScore;
    private String icfLabel;
    private String icfDirection;
    private String riskLevel;
    private Boolean sentinelActive;
    private Boolean planSummaryAvailable;
    private Boolean hasActiveSprint;
    private String activeSprintStatus;
    private Boolean crisisHistoryAvailable;

    // ── Solo para TERRITORIAL (datos anónimos geográficos) ────────────────
    private Boolean aggregatedOnly;
    private String municipio;
    private String departmentCode;
    private String countryCode;
}
