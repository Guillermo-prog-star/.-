package com.integrityfamily.domain.repository;

import com.integrityfamily.domain.Family;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FamilyRepository extends JpaRepository<Family, Long> {

    @Query("SELECT DISTINCT f FROM Family f LEFT JOIN FETCH f.members")
    List<Family> findAll();

    @Query("SELECT DISTINCT f FROM Family f LEFT JOIN FETCH f.members WHERE f.familyCode = :familyCode")
    Optional<Family> findByFamilyCodeWithMembers(@Param("familyCode") String familyCode);

    @Query("SELECT DISTINCT f FROM Family f LEFT JOIN FETCH f.members WHERE f.createdBy.email = :email")
    Optional<Family> findByCreatedByEmailWithMembers(@Param("email") String email);

    @Query("SELECT DISTINCT f FROM Family f LEFT JOIN FETCH f.members WHERE f.id = :id")
    Optional<Family> findByIdWithMembers(@Param("id") Long id);

    boolean existsByFamilyCode(String familyCode);

    Optional<Family> findByCreatedBy_Email(String email);

    List<FamilySummary> findProjectedBy();

    Optional<FamilySummary> findProjectedById(Long id);

    List<Family> findByNextEvaluationAtBeforeOrNextEvaluationAtIsNull(LocalDateTime now);

    List<Family> findBySentinelActiveTrue();

    Optional<Family> findByFamilyCode(String familyCode);
    
    List<Family> findByName(String name);

    List<Family> findByMunicipio(String municipio);
}
