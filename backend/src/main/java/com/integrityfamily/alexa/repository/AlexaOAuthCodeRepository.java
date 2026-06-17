package com.integrityfamily.alexa.repository;

import com.integrityfamily.alexa.domain.AlexaOAuthCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface AlexaOAuthCodeRepository extends JpaRepository<AlexaOAuthCode, String> {

    Optional<AlexaOAuthCode> findByCodeAndUsedFalse(String code);

    @Transactional
    @Modifying
    @Query("DELETE FROM AlexaOAuthCode c WHERE c.expiresAt < :now")
    void deleteExpired(long now);
}
