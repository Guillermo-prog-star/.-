package com.integrityfamily.domain.repository;

import com.integrityfamily.domain.FamilyCriticalEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FamilyCriticalEventRepository extends JpaRepository<FamilyCriticalEvent, Long> {

    List<FamilyCriticalEvent> findByFamilyIdOrderByDetectedAtDesc(Long familyId);

    /** Eventos activos de una familia (requieren seguimiento) */
    @Query("SELECT e FROM FamilyCriticalEvent e WHERE e.family.id = :familyId " +
           "AND e.status IN ('DETECTED', 'IN_PROGRESS', 'RELAPSED') ORDER BY e.detectedAt DESC")
    List<FamilyCriticalEvent> findActiveByFamilyId(@Param("familyId") Long familyId);

    /** Eventos resueltos de una familia — para calcular resiliencia */
    @Query("SELECT e FROM FamilyCriticalEvent e WHERE e.family.id = :familyId " +
           "AND e.status IN ('RESOLVED', 'CLOSED') ORDER BY e.resolvedAt DESC")
    List<FamilyCriticalEvent> findResolvedByFamilyId(@Param("familyId") Long familyId);

    /** Total de eventos de una familia por estado */
    @Query("SELECT COUNT(e) FROM FamilyCriticalEvent e WHERE e.family.id = :familyId AND e.status = :status")
    long countByFamilyIdAndStatus(@Param("familyId") Long familyId, @Param("status") String status);

    /** Total de eventos activos */
    @Query("SELECT COUNT(e) FROM FamilyCriticalEvent e WHERE e.family.id = :familyId " +
           "AND e.status IN ('DETECTED', 'IN_PROGRESS', 'RELAPSED')")
    long countActiveByFamilyId(@Param("familyId") Long familyId);

    /** Promedio de días hasta resolución para una familia */
    @Query("SELECT COALESCE(AVG(e.daysToResolution), 0) FROM FamilyCriticalEvent e " +
           "WHERE e.family.id = :familyId AND e.daysToResolution IS NOT NULL")
    Double avgDaysToResolutionByFamilyId(@Param("familyId") Long familyId);

    /** Total de recaídas acumuladas */
    @Query("SELECT COALESCE(SUM(e.relapseCount), 0) FROM FamilyCriticalEvent e WHERE e.family.id = :familyId")
    Long totalRelapsesByFamilyId(@Param("familyId") Long familyId);

    /** Buscar por criticalDayId para vincular con el sistema de crisis existente */
    Optional<FamilyCriticalEvent> findByFamilyIdAndCriticalDayId(Long familyId, Long criticalDayId);

    // ── Para el Observatorio ──────────────────────────────────────────────────

    /** Todos los eventos detectados en un rango de fechas */
    @Query("SELECT e FROM FamilyCriticalEvent e WHERE e.detectedAt >= :from AND e.detectedAt <= :to")
    List<FamilyCriticalEvent> findDetectedInRange(
            @Param("from") LocalDate from, @Param("to") LocalDate to);

    /** Todos los resueltos en un rango de fechas */
    @Query("SELECT e FROM FamilyCriticalEvent e WHERE e.resolvedAt >= :from AND e.resolvedAt <= :to")
    List<FamilyCriticalEvent> findResolvedInRange(
            @Param("from") LocalDate from, @Param("to") LocalDate to);
}
