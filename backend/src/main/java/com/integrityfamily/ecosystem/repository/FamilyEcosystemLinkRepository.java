package com.integrityfamily.ecosystem.repository;

import com.integrityfamily.ecosystem.domain.EcosystemLinkStatus;
import com.integrityfamily.ecosystem.domain.FamilyEcosystemLink;
import com.integrityfamily.ecosystem.domain.NetworkType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FamilyEcosystemLinkRepository extends JpaRepository<FamilyEcosystemLink, Long> {
    List<FamilyEcosystemLink> findByFamilyId(Long familyId);
    List<FamilyEcosystemLink> findByFamilyIdAndStatus(Long familyId, EcosystemLinkStatus status);
    List<FamilyEcosystemLink> findByFamilyIdAndNetworkType(Long familyId, NetworkType networkType);
    boolean existsByFamilyIdAndParticipantIdAndStatusNot(Long familyId, Long participantId, EcosystemLinkStatus status);

    @Query("SELECT COUNT(l) > 0 FROM FamilyEcosystemLink l WHERE LOWER(l.participant.contactEmail) = LOWER(:email) AND l.status = :status")
    boolean existsByParticipantContactEmailAndStatus(@Param("email") String email, @Param("status") EcosystemLinkStatus status);

    @Query("SELECT l FROM FamilyEcosystemLink l WHERE LOWER(l.participant.contactEmail) = LOWER(:email) AND l.status = :status")
    List<FamilyEcosystemLink> findByParticipantContactEmailAndStatus(@Param("email") String email, @Param("status") EcosystemLinkStatus status);
}
