package com.integrityfamily.domain.repository;

import com.integrityfamily.domain.JournalEntry;
import com.integrityfamily.domain.JournalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {
    List<JournalEntry> findByFamilyIdOrderByCreatedAtDesc(Long familyId);
    List<JournalEntry> findByFamilyIdAndStatusOrderByCreatedAtDesc(Long familyId, JournalStatus status);

    /**
     * Filtro de visibilidad (ADR-012, H2): visible si esta compartida con la
     * familia, si no tiene autor (generada por sistema/IA), o si el autor es
     * el propio viewer. viewerMemberId null = sin filtrar (bypass admin/creador,
     * ver SecurityValidator.resolveViewerMemberId).
     */
    @Query("SELECT j FROM JournalEntry j WHERE j.family.id = :familyId " +
           "AND (:viewerMemberId IS NULL " +
           "     OR j.visibility = com.integrityfamily.domain.EntryVisibility.SHARED_WITH_FAMILY " +
           "     OR j.member IS NULL " +
           "     OR j.member.id = :viewerMemberId) " +
           "ORDER BY j.createdAt DESC")
    List<JournalEntry> findVisibleToMember(@Param("familyId") Long familyId, @Param("viewerMemberId") Long viewerMemberId);
}
