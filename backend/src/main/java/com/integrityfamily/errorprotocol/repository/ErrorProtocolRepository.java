package com.integrityfamily.errorprotocol.repository;

import com.integrityfamily.errorprotocol.domain.FamilyErrorProtocol;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ErrorProtocolRepository extends JpaRepository<FamilyErrorProtocol, Long> {
    List<FamilyErrorProtocol> findByFamilyIdOrderByCreatedAtDesc(Long familyId);
    List<FamilyErrorProtocol> findByFamilyIdAndClosedFalse(Long familyId);
}
