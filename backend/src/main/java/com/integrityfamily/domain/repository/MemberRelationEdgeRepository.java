package com.integrityfamily.domain.repository;

import com.integrityfamily.domain.MemberRelationEdge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MemberRelationEdgeRepository extends JpaRepository<MemberRelationEdge, Long> {

    List<MemberRelationEdge> findByFamilyId(Long familyId);

    /** Busca el par sin importar el orden (siempre memberA.id < memberB.id) */
    @Query("SELECT e FROM MemberRelationEdge e " +
           "WHERE e.family.id = :familyId " +
           "AND e.memberA.id = :aId AND e.memberB.id = :bId")
    Optional<MemberRelationEdge> findPair(@Param("familyId") Long familyId,
                                          @Param("aId") Long aId,
                                          @Param("bId") Long bId);

    /** Todas las aristas en las que participa un miembro concreto */
    @Query("SELECT e FROM MemberRelationEdge e " +
           "WHERE e.family.id = :familyId " +
           "AND (e.memberA.id = :memberId OR e.memberB.id = :memberId)")
    List<MemberRelationEdge> findEdgesOfMember(@Param("familyId") Long familyId,
                                               @Param("memberId") Long memberId);

    /** Aristas con dinámica conflictiva */
    List<MemberRelationEdge> findByFamilyIdAndDynamicType(
            Long familyId, MemberRelationEdge.DynamicType dynamicType);

    int countByFamilyId(Long familyId);
}
