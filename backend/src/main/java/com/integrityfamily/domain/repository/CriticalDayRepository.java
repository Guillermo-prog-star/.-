package com.integrityfamily.domain.repository;

import com.integrityfamily.domain.CriticalDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CriticalDayRepository extends JpaRepository<CriticalDay, Long> {
    List<CriticalDay> findByFamilyIdOrderByCreatedAtDesc(Long familyId);

    /**
     * Filtro de visibilidad (ADR-012, H2): visible si esta compartida con la
     * familia, si no tiene autor (ej. SENTINEL_ALERT generada por IA), o si
     * el autor es el propio viewer. viewerMemberId null = sin filtrar
     * (bypass admin/creador, ver SecurityValidator.resolveViewerMemberId).
     */
    @Query("SELECT c FROM CriticalDay c WHERE c.familyId = :familyId " +
           "AND (:viewerMemberId IS NULL " +
           "     OR c.visibility = com.integrityfamily.domain.EntryVisibility.SHARED_WITH_FAMILY " +
           "     OR c.memberId IS NULL " +
           "     OR c.memberId = :viewerMemberId) " +
           "ORDER BY c.createdAt DESC")
    List<CriticalDay> findVisibleToMember(@Param("familyId") Long familyId, @Param("viewerMemberId") Long viewerMemberId);
}
