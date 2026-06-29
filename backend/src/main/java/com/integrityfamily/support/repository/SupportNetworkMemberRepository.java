package com.integrityfamily.support.repository;

import com.integrityfamily.support.domain.SupportNetworkMember;
import com.integrityfamily.support.domain.SupportSpecialty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupportNetworkMemberRepository extends JpaRepository<SupportNetworkMember, Long> {
    Optional<SupportNetworkMember> findByEmail(String email);
    List<SupportNetworkMember> findBySpecialtyAndActiveTrue(SupportSpecialty specialty);
    List<SupportNetworkMember> findByActiveTrue();
    boolean existsByEmail(String email);
}
