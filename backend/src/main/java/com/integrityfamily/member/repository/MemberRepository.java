package com.integrityfamily.member.repository;

import com.integrityfamily.family.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * MemberRepository: Interfaz de acceso a datos para los integrantes de la familia.
 * Utiliza Spring Data JPA para gestionar la persistencia en el Nodo Armenia.
 */
@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    
    /**
     * Recupera todos los miembros pertenecientes a una familia específica.
     * Spring Data JPA genera automáticamente: SELECT * FROM members WHERE family_id = ?
     * * @param familyId Identificador único de la familia.
     * @return Lista de integrantes encontrados.
     */
    List<Member> findByFamilyId(Long familyId);
}