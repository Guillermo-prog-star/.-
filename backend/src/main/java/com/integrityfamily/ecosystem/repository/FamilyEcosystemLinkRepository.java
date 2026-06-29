package com.integrityfamily.ecosystem.repository;

import com.integrityfamily.ecosystem.domain.EcosystemLinkStatus;
import com.integrityfamily.ecosystem.domain.FamilyEcosystemLink;
import com.integrityfamily.ecosystem.domain.NetworkType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FamilyEcosystemLinkRepository extends JpaRepository<FamilyEcosystemLink, Long> {
    List<FamilyEcosystemLink> findByFamilyId(Long familyId);
    List<FamilyEcosystemLink> findByFamilyIdAndStatus(Long familyId, EcosystemLinkStatus status);
    List<FamilyEcosystemLink> findByFamilyIdAndNetworkType(Long familyId, NetworkType networkType);
    boolean existsByFamilyIdAndParticipantIdAndStatusNot(Long familyId, Long participantId, EcosystemLinkStatus status);
}
