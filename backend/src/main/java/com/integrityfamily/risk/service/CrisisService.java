package com.integrityfamily.risk.service;

import com.integrityfamily.domain.FamilyMember;
import com.integrityfamily.domain.CriticalDay;
import java.util.List;

/**
 * SDD CONTRACT: Gestión de Protocolos de Crisis Sentinel & Registro Histórico.
 * Integra el registro de eventos individuales con la respuesta sistémica.
 */
public interface CrisisService {

    /**
     * SDD-RECOVERY: Registra un evento crítico individual y retorna la entidad
     * persistida.
     * Recupera la compatibilidad con el flujo actual del CrisisController.
     */
    CriticalDay registerCrisis(Long familyId, Long memberId, String category, String description, String emotion);

    /**
     * SDD-RECOVERY: Recupera el historial de días críticos para una familia.
     */
    List<CriticalDay> getHistory(Long familyId);

    /**
     * Activa el protocolo de emergencia para una familia.
     */
    void activateProtocol(Long familyId, String reason);

    /**
     * Procesa la involucración de miembros específicos en un evento crítico.
     */
    void handleMemberCrisis(Long familyId, List<FamilyMember> involvedMembers, String observation);

    /**
     * Verifica si una familia tiene actualmente el protocolo Sentinel activo.
     */
    boolean isUnderCrisis(Long familyId);
}


