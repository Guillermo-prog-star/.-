package com.integrityfamily.guardian.repository;

import com.integrityfamily.guardian.domain.GuardianVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface GuardianVoteRepository extends JpaRepository<GuardianVote, Long> {

    boolean existsByFamilyIdAndVoterId(Long familyId, Long voterId);

    Optional<GuardianVote> findByFamilyIdAndVoterId(Long familyId, Long voterId);

    /** Cuenta votos por candidato para una familia. */
    @Query("SELECT v.nominated.id, COUNT(v) FROM GuardianVote v WHERE v.family.id = :familyId GROUP BY v.nominated.id ORDER BY COUNT(v) DESC")
    List<Object[]> countVotesByFamilyGroupedByNominated(@Param("familyId") Long familyId);

    long countByFamilyId(Long familyId);
}
