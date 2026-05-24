package com.integrityfamily.domain.repository;

import com.integrityfamily.domain.EmotionalStimulus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface EmotionalStimulusRepository extends JpaRepository<EmotionalStimulus, Long> {

    /**
     * Obtiene el estímulo más reciente para un rol objetivo y categoría específica.
     */
    Optional<EmotionalStimulus> findFirstByTargetRoleAndCategoryOrderByCreatedAtDesc(String targetRole, String category);

    /**
     * Obtiene el estímulo de video/audio más reciente por tipo.
     */
    Optional<EmotionalStimulus> findFirstByTypeOrderByCreatedAtDesc(String type);
}
