package com.integrityfamily.checklist.repository;

import com.integrityfamily.checklist.domain.ChecklistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * ChecklistItemRepository: Punto de acceso a datos para los hitos de control.
 * Sincronizado con la auditoría 'createdAt' para un despliegue sin errores.
 */
@Repository
public interface ChecklistItemRepository extends JpaRepository<ChecklistItem, Long> {

    /**
     * Recupera los ítems de checklist de una familia ordenados por fecha de creación (más recientes primero).
     * Este método requiere que 'createdAt' exista en ChecklistItem.java.
     */
    List<ChecklistItem> findByFamilyIdOrderByCreatedAtDesc(Long familyId);

    /**
     * Busca ítems filtrando por el Plan de Acción asociado.
     */
    List<ChecklistItem> findByPlanId(Long planId);
}