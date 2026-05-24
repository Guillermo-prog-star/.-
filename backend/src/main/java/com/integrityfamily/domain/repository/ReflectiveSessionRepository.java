package com.integrityfamily.domain.repository;

import com.integrityfamily.domain.ReflectiveSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReflectiveSessionRepository extends JpaRepository<ReflectiveSession, Long> {

    /**
     * Obtiene el historial de reflexiones de una familia ordenadas de más reciente a más antigua.
     */
    List<ReflectiveSession> findByFamilyIdOrderByCreatedAtDesc(Long familyId);

    /**
     * Obtiene el historial de reflexiones de un miembro particular en una familia.
     */
    List<ReflectiveSession> findByFamilyIdAndMemberIdOrderByCreatedAtDesc(Long familyId, Long memberId);

    /**
     * Obtiene si un miembro ya reflexionó sobre un estímulo particular para prevenir duplicaciones.
     */
    Optional<ReflectiveSession> findFirstByFamilyIdAndMemberIdAndStimulusId(Long familyId, Long memberId, Long stimulusId);
}
