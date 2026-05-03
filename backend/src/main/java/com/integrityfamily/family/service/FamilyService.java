package com.integrityfamily.family.service;

import com.integrityfamily.domain.User;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.repository.UserRepository;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * FamilyService: Motor de GestiÃƒÂ³n y VisualizaciÃƒÂ³n del Nodo Familiar.
 * Optimizado para carga masiva de integrantes y trazabilidad con IA.
 */
@Service
public class FamilyService {

    private final FamilyRepository familyRepository;
    private final UserRepository userRepository;

    public FamilyService(FamilyRepository familyRepository, UserRepository userRepository) {
        this.familyRepository = familyRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public java.util.Optional<Family> findByCreatorEmail(String email) {
        return familyRepository.findByCreatedBy_Email(email);
    }

    /**
     * Recupera el nÃƒÂºcleo familiar completo con todos sus integrantes.
     * Utiliza la consulta optimizada JOIN FETCH para evitar latencia.
     */
    @Transactional(readOnly = true)
    public Family getFullFamilyContext(String email) {
        return familyRepository.findByCreatedByEmailWithMembers(email)
                .orElseThrow(() -> new RuntimeException("No se encontrÃƒÂ³ un nÃƒÂºcleo familiar asociado a: " + email));
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
     * Crea un nuevo nÃƒÂºcleo familiar vinculÃƒÂ¡ndolo al usuario y generando el cÃƒÂ³digo de nodo.
     * Formato requerido: IF-CO-QUI-{YEAR}-{SEQUENCE}
     */
    @Transactional
    public Family create(Family family, String creatorEmail) {
        User creator = userRepository.findByEmail(creatorEmail)
                .orElseThrow(() -> new BusinessException("Usuario creador no encontrado", "USER_NOT_FOUND", org.springframework.http.HttpStatus.NOT_FOUND));

        if (familyRepository.findByCreatedBy_Email(creatorEmail).isPresent()) {
            throw new BusinessException("El usuario ya posee un nÃƒÂºcleo familiar registrado.", "ALREADY_HAS_FAMILY", org.springframework.http.HttpStatus.CONFLICT);
        }

        // GeneraciÃƒÂ³n de cÃƒÂ³digo regional estratÃƒÂ©gico
        int currentYear = java.time.Year.now().getValue();
        long count = familyRepository.count() + 1;
        String sequence = String.format("%04d", count);
        String familyCode = "IF-CO-QUI-" + currentYear + "-" + sequence;
        
        family.setFamilyCode(familyCode);
        family.setCreatedBy(creator);
        family.setCurrentMilestone("MES_00_DIAGNOSTICO_BASE");
        
        Family saved = familyRepository.save(family);
        
        // SDD: Sincronización de Identidad. Vinculamos al creador con su nuevo nodo.
        creator.setFamily(saved);
        userRepository.save(creator);
        
        return saved;
    }

    /**
     * Actualiza los datos del nÃƒÂºcleo manteniendo la integridad del cÃƒÂ³digo FAM.
     */
    @Transactional
    public Family update(Long id, Family request) {
        Family existing = findById(id); // Usa la bÃƒÂºsqueda optimizada
        
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


