package com.integrityfamily.ecosystem.domain;

public enum EcosystemLinkStatus {
    INVITED,    // La familia envió la invitación; el participante aún no acepta
    ACTIVE,     // Consentimiento otorgado; acceso vigente
    SUSPENDED,  // Acceso pausado temporalmente
    REVOKED     // Acceso revocado definitivamente por la familia
}
