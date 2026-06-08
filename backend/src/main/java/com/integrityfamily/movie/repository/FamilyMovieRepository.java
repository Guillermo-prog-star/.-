package com.integrityfamily.movie.repository;

import com.integrityfamily.movie.domain.FamilyMovie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FamilyMovieRepository extends JpaRepository<FamilyMovie, Long> {
    List<FamilyMovie> findByFamilyIdOrderByPeriodStartDesc(Long familyId);
    Optional<FamilyMovie> findFirstByFamilyIdOrderByPeriodStartDesc(Long familyId);
    boolean existsByFamilyIdAndPeriodStartAndPeriodEnd(Long familyId, LocalDate start, LocalDate end);
}
