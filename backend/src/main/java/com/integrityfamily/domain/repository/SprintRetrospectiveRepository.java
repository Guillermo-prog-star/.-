package com.integrityfamily.domain.repository;

import com.integrityfamily.domain.SprintRetrospective;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SprintRetrospectiveRepository extends JpaRepository<SprintRetrospective, Long> {
    Optional<SprintRetrospective> findBySprintId(Long sprintId);
}
