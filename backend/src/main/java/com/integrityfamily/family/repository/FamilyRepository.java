package com.integrityfamily.family.repository;

import com.integrityfamily.family.domain.Family;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * FamilyRepository: Puente optimizado con JOIN FETCH.
 * Reduce la latencia al cargar el núcleo familiar y sus integrantes en un solo Query.
 */
@Repository
public interface FamilyRepository extends JpaRepository<Family, Long> {

    /**
     * Busca por código de nodo cargando integrantes (Optimizado para IA).
     */
    @Query("SELECT f FROM Family f LEFT JOIN FETCH f.members WHERE f.familyCode = :familyCode")
    Optional<Family> findByFamilyCodeWithMembers(@Param("familyCode") String familyCode);

    /**
     * Busca la familia del usuario (William) cargando todos los miembros de golpe.
     * Evita múltiples consultas a MySQL.
     */
    @Query("SELECT f FROM Family f LEFT JOIN FETCH f.members WHERE f.createdBy.email = :email")
    Optional<Family> findByCreatedByEmailWithMembers(@Param("email") String email);

    /**
     * Busca por ID cargando integrantes.
     */
    @Query("SELECT f FROM Family f LEFT JOIN FETCH f.members WHERE f.id = :id")
    Optional<Family> findByIdWithMembers(@Param("id") Long id);

    boolean existsByFamilyCode(String familyCode);

    Optional<Family> findByCreatedBy_Email(String email);
}