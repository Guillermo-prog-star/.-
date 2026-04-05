package com.integrityfamily.family.service;

import com.integrityfamily.auth.domain.User;
import com.integrityfamily.auth.repository.UserRepository;
import com.integrityfamily.family.domain.Family;
import com.integrityfamily.family.repository.FamilyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * FamilyService: Motor de Gestión y Visualización del Nodo Familiar.
 * Optimizado para carga masiva de integrantes y trazabilidad con IA.
 */
@Service
@RequiredArgsConstructor
public class FamilyService {

    private final FamilyRepository familyRepository;
    private final UserRepository userRepository;

    /**
     * Recupera el núcleo familiar completo (William) con todos sus integrantes.
     * Utiliza la consulta optimizada JOIN FETCH para evitar latencia.
     */
    @Transactional(readOnly = true)
    public Family getFullFamilyContext(String email) {
        return familyRepository.findByCreatedByEmailWithMembers(email)
                .orElseThrow(() -> new RuntimeException("No se encontró un núcleo familiar asociado a: " + email));
    }

    /**
     * Busca una familia por ID cargando sus miembros de golpe para el Dashboard.
     */
    @Transactional(readOnly = true)
    public Family findById(Long id) {
        return familyRepository.findByIdWithMembers(id)
                .orElseThrow(() -> new RuntimeException("Familia con ID " + id + " no encontrada"));
    }

    /**
     * Crea un nuevo núcleo familiar vinculándolo al usuario y generando el código de nodo.
     */
    @Transactional
    public Family create(Family family, String creatorEmail) {
        User creator = userRepository.findByEmail(creatorEmail)
                .orElseThrow(() -> new RuntimeException("Usuario creador no encontrado"));

        if (familyRepository.findByCreatedBy_Email(creatorEmail).isPresent()) {
            throw new RuntimeException("El usuario ya posee un núcleo familiar registrado.");
        }

        // Generación de código único FAM-XXXX para trazabilidad con IA
        String shortCode = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        family.setFamilyCode("FAM-" + shortCode);
        family.setCreatedBy(creator);
        family.setCurrentMilestone("DIAGNOSTICO_INICIAL");
        
        return familyRepository.save(family);
    }

    /**
     * Actualiza los datos del núcleo manteniendo la integridad del código FAM.
     */
    @Transactional
    public Family update(Long id, Family request) {
        Family existing = findById(id); // Usa la búsqueda optimizada
        
        existing.setName(request.getName());
        existing.setDescription(request.getDescription());
        existing.setMunicipio(request.getMunicipio());
        existing.setWhatsapp(request.getWhatsapp());
        existing.setPin(request.getPin());

        if (request.getCurrentMilestone() != null) {
            existing.setCurrentMilestone(request.getCurrentMilestone());
        }

        return familyRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        if (!familyRepository.existsById(id)) {
            throw new RuntimeException("La familia no existe.");
        }
        familyRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<Family> findAll() {
        return familyRepository.findAll();
    }
}