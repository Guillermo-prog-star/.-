package com.integrityfamily.domain.repository;

import com.integrityfamily.domain.FailedLoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;

@Repository
public interface FailedLoginAttemptRepository extends JpaRepository<FailedLoginAttempt, Long> {

    @Query("SELECT COUNT(f) FROM FailedLoginAttempt f WHERE LOWER(f.email)=LOWER(:email) AND f.attemptedAt >= :since")
    long countRecentByEmail(@Param("email") String email, @Param("since") LocalDateTime since);

    @Modifying
    @Query("DELETE FROM FailedLoginAttempt f WHERE LOWER(f.email)=LOWER(:email)")
    void deleteAllByEmail(@Param("email") String email);
}
