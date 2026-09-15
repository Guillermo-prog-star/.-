package com.integrityfamily.consent.repository;

import com.integrityfamily.consent.domain.Consent;
import com.integrityfamily.consent.domain.ConsentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConsentRepository extends JpaRepository<Consent, Long> {
    List<Consent> findByFamilyIdOrderByCreatedAtDesc(Long familyId);
    List<Consent> findByFamilyIdAndStatus(Long familyId, ConsentStatus status);
}
