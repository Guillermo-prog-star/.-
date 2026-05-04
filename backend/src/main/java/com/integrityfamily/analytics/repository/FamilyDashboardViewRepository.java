package com.integrityfamily.analytics.repository;

import com.integrityfamily.analytics.domain.FamilyDashboardView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * SDD: Repositorio de acceso rápido para el Read Model del Dashboard.
 */
@Repository
public interface FamilyDashboardViewRepository extends JpaRepository<FamilyDashboardView, Long> {
}
