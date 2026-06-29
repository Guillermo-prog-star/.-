package com.integrityfamily.ecosystem.repository;

import com.integrityfamily.ecosystem.domain.EcosystemParticipantContact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EcosystemParticipantContactRepository extends JpaRepository<EcosystemParticipantContact, Long> {
    List<EcosystemParticipantContact> findByParticipantIdAndActiveTrue(Long participantId);
}
