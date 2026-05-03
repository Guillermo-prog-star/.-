package com.integrityfamily.domain.repository;

import com.integrityfamily.domain.AdminAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface AdminAlertRepository extends JpaRepository<AdminAlert, Long> {
    Optional<AdminAlert> findByTitleAndMessage(String title, String message);
    List<AdminAlert> findAllByOrderByCreatedAtDesc();
}
