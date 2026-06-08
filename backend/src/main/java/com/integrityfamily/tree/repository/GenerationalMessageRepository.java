package com.integrityfamily.tree.repository;

import com.integrityfamily.tree.domain.GenerationalMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GenerationalMessageRepository extends JpaRepository<GenerationalMessage, Long> {
    List<GenerationalMessage> findByFromFamilyIdOrderByCreatedAtDesc(Long fromFamilyId);

    /** Mensajes dirigidos a esta familia o a todas las generaciones (toFamilyId null). */
    @Query("SELECT m FROM GenerationalMessage m WHERE m.toFamilyId = :familyId OR m.toFamilyId IS NULL ORDER BY m.createdAt DESC")
    List<GenerationalMessage> findForFamily(Long familyId);
}
