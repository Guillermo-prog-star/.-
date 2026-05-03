package com.integrityfamily.risk.service;

import com.integrityfamily.domain.FamilyMember;
import com.integrityfamily.domain.CriticalDay;
import java.util.List;

/**
 * SDD CONTRACT: GestiÃƒÂ³n de Protocolos de Crisis Sentinel & Registro HistÃƒÂ³rico.
 * Integra el registro de eventos individuales con la respuesta sistÃƒÂ©mica.
 */
public interface CrisisService {

    /**
     * SDD-RECOVERY: Registra un evento crÃƒÂ­tico individual y retorna la entidad
     * persistida.
     * Recupera la compatibilidad con el flujo actual del CrisisController.
     */
    CriticalDay registerCrisis(Long familyId, Long memberId, String category, String description, String emotion);

    /**
     * SDD-RECOVERY: Recupera el historial de dÃƒÂ­as crÃƒÂ­ticos para una familia.
     */
    List<CriticalDay> getHistory(Long familyId);

    /**
     * Activa el protocolo de emergencia para una familia.
     */
    void activateProtocol(Long familyId, String reason);

    /**
     * Procesa la involucraciÃƒÂ³n de miembros especÃƒÂ­ficos en un evento crÃƒÂ­tico.
     */
    void handleMemberCrisis(Long familyId, List<FamilyMember> involvedMembers, String observation);

    /**
     * Verifica si una familia tiene actualmente el protocolo Sentinel activo.
     */
    boolean isUnderCrisis(Long familyId);
}


