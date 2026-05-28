package com.integrityfamily.domain.repository;

import com.integrityfamily.domain.MemberIdentityProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberIdentityProfileRepository extends JpaRepository<MemberIdentityProfile, Long> {

    Optional<MemberIdentityProfile> findByMemberId(Long memberId);
}
