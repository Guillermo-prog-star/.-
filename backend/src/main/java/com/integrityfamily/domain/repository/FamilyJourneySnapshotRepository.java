package com.integrityfamily.domain.repository;

import com.integrityfamily.domain.FamilyJourneySnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FamilyJourneySnapshotRepository extends JpaRepository<FamilyJourneySnapshot, Long> {

    /** Último snapshot de una familia (para detectar level-up). */
    Optional<FamilyJourneySnapshot> findTopByFamilyIdOrderBySnapshotDateDesc(Long familyId);

    /** Snapshots pendientes de celebración (para el scheduler). */
    List<FamilyJourneySnapshot> findByLevelUpTrueAndCelebrationSentFalse();

    /** Historial completo de una familia en orden cronológico. */
    List<FamilyJourneySnapshot> findByFamilyIdOrderBySnapshotDateAsc(Long familyId);

    /** Comprueba si ya hay snapshot para hoy (evita duplicados). */
    boolean existsByFamilyIdAndSnapshotDate(Long familyId, LocalDate date);
}
