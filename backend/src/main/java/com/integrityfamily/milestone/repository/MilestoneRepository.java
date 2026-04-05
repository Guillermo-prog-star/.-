package com.integrityfamily.milestone.repository;
import com.integrityfamily.milestone.domain.Milestone;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List; import java.util.Optional;
public interface MilestoneRepository extends JpaRepository<Milestone,Long> {
    Optional<Milestone> findByMilestoneKey(String key);
    List<Milestone> findAllByOrderBySortOrderAsc();
}
