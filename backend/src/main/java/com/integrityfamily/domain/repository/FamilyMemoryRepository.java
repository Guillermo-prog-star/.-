package com.integrityfamily.domain.repository;

import com.integrityfamily.domain.FamilyMemory;
import com.integrityfamily.domain.FamilyMemory.MemoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FamilyMemoryRepository extends JpaRepository<FamilyMemory, Long> {

    List<FamilyMemory> findByFamilyIdOrderByImportanceScoreDescCreatedAtDesc(Long familyId);

    List<FamilyMemory> findByFamilyIdAndMemoryTypeOrderByImportanceScoreDesc(Long familyId, MemoryType memoryType);

    List<FamilyMemory> findByFamilyIdAndSemanticKeyOrderByCreatedAtDesc(Long familyId, String semanticKey);

    /** Recupera el contexto cognitivo compacto: las N memorias más relevantes de la familia */
    @Query("SELECT m FROM FamilyMemory m WHERE m.family.id = :familyId " +
           "AND (m.expiresAt IS NULL OR m.expiresAt > :now) " +
           "ORDER BY m.importanceScore DESC, m.createdAt DESC")
    List<FamilyMemory> findActiveMemoriesByFamilyId(
            @Param("familyId") Long familyId,
            @Param("now") LocalDateTime now);

    /** Memorias semánticas: patrones consolidados de la familia */
    @Query("SELECT m FROM FamilyMemory m WHERE m.family.id = :familyId " +
           "AND m.memoryType = 'SEMANTIC' ORDER BY m.importanceScore DESC")
    List<FamilyMemory> findSemanticPatterns(@Param("familyId") Long familyId);

    /** Memorias procedurales: habilidades activas para esta familia */
    @Query("SELECT m FROM FamilyMemory m WHERE m.family.id = :familyId " +
           "AND m.memoryType = 'PROCEDURAL' ORDER BY m.createdAt DESC")
    List<FamilyMemory> findProceduralMemories(@Param("familyId") Long familyId);

    void deleteByFamilyIdAndExpiresAtBefore(Long familyId, LocalDateTime cutoff);
}
