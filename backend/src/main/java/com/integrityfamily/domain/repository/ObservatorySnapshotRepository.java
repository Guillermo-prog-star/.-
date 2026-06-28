package com.integrityfamily.domain.repository;

import com.integrityfamily.domain.ObservatorySnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ObservatorySnapshotRepository extends JpaRepository<ObservatorySnapshot, Long> {

    Optional<ObservatorySnapshot> findBySnapshotMonth(LocalDate snapshotMonth);

    /** Últimos N meses — para series de tiempo en dashboard del Observatorio */
    @Query("SELECT o FROM ObservatorySnapshot o ORDER BY o.snapshotMonth DESC")
    List<ObservatorySnapshot> findAllOrderByMonthDesc();

    /** Rango de fechas para comparativos longitudinales */
    @Query("SELECT o FROM ObservatorySnapshot o " +
           "WHERE o.snapshotMonth >= :from AND o.snapshotMonth <= :to " +
           "ORDER BY o.snapshotMonth ASC")
    List<ObservatorySnapshot> findInRange(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    /** Snapshot más reciente disponible */
    @Query("SELECT o FROM ObservatorySnapshot o ORDER BY o.snapshotMonth DESC LIMIT 1")
    Optional<ObservatorySnapshot> findLatest();

    boolean existsBySnapshotMonth(LocalDate snapshotMonth);
}
