package com.integrityfamily.domain.repository;

import com.integrityfamily.domain.Reflection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReflectionRepository extends JpaRepository<Reflection, Long> {
    List<Reflection> findByFamilyId(Long familyId);
    List<Reflection> findByTaskId(Long taskId);
}
