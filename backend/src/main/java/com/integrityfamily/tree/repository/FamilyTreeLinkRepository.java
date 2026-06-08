package com.integrityfamily.tree.repository;

import com.integrityfamily.tree.domain.FamilyTreeLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface FamilyTreeLinkRepository extends JpaRepository<FamilyTreeLink, Long> {
    /** Familia origen de esta familia (padre directo). */
    Optional<FamilyTreeLink> findByChildFamilyId(Long childFamilyId);

    /** Familias descendientes directas de esta. */
    List<FamilyTreeLink> findByParentFamilyId(Long parentFamilyId);

    boolean existsByChildFamilyId(Long childFamilyId);
    boolean existsByParentFamilyIdAndChildFamilyId(Long parentFamilyId, Long childFamilyId);
}
