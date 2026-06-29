package com.integrityfamily.ecosystem.repository;

import com.integrityfamily.ecosystem.domain.EcosystemParticipant;
import com.integrityfamily.ecosystem.domain.NetworkType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EcosystemParticipantRepository extends JpaRepository<EcosystemParticipant, Long> {
    List<EcosystemParticipant> findByActiveTrue();
    List<EcosystemParticipant> findByNetworkTypeAndActiveTrue(NetworkType networkType);
    boolean existsByNameAndNetworkType(String name, NetworkType networkType);
}
