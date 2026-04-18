package com.integrityfamily.milestone.repository;

import com.integrityfamily.milestone.domain.Milestone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MilestoneRepository extends JpaRepository<Milestone, Long> {

    /**
     * Recupera un hito por su clave de negocio única.
     */
    Optional<Milestone> findByMilestoneKey(String milestoneKey);

    /**
     * Recupera todos los hitos ordenados por su orden de clasificación.
     * Vital para la visualización secuencial en el frontend.
     */
    List<Milestone> findAllByOrderBySortOrderAsc();

    /**
     * OPTIMIZACIÓN: Verifica existencia sin cargar datos en memoria.
     * Ideal para validaciones en lógica de negocio.
     */
    boolean existsByMilestoneKey(String milestoneKey);
}