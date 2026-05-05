package com.integrityfamily.family.service;

import com.integrityfamily.domain.User;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.FamilyMember;
import com.integrityfamily.family.dto.FamilyMemberResponse;
import com.integrityfamily.family.dto.FamilyResponse;
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
    public java.util.Optional<FamilyResponse> findByCreatorEmail(String email) {
        return familyRepository.findByCreatedBy_Email(email).map(this::toResponse);
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
    public FamilyResponse findById(Long id) {
        Family family = familyRepository.findByIdWithMembers(id)
                .orElseThrow(() -> new RuntimeException("Familia con ID " + id + " no encontrada"));
        return toResponse(family);
    }

    /**
     * Crea un nuevo nÃƒÂºcleo familiar vinculÃƒÂ¡ndolo al usuario y generando el cÃƒÂ³digo de nodo.
     * Formato requerido: IF-CO-QUI-{YEAR}-{SEQUENCE}
     */
    @Transactional
    public FamilyResponse create(Family family, String creatorEmail) {
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
        userRepository.saveAndFlush(creator);
        
        return toResponse(saved);
    }

    /**
     * Actualiza los datos del nÃƒÂºcleo manteniendo la integridad del cÃƒÂ³digo FAM.
     */
    @Transactional
    public FamilyResponse update(Long id, Family request) {
        Family existing = familyRepository.findById(id) // Búsqueda directa para actualización
                .orElseThrow(() -> new RuntimeException("Familia con ID " + id + " no encontrada"));
        
        existing.setName(request.getName());
        existing.setDescription(request.getDescription());
        existing.setMunicipio(request.getMunicipio());
        existing.setWhatsapp(request.getWhatsapp());
        existing.setPin(request.getPin());

        if (request.getCurrentMilestone() != null) {
            existing.setCurrentMilestone(request.getCurrentMilestone());
        }

        return toResponse(familyRepository.save(existing));
    }

    @Transactional
    public void delete(Long id) {
        if (!familyRepository.existsById(id)) {
            throw new RuntimeException("La familia no existe.");
        }
        familyRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<FamilyResponse> findAll() {
        return familyRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Mapea una entidad Family a su DTO de respuesta.
     */
    public FamilyResponse toResponse(Family family) {
        if (family == null) return null;

        List<FamilyMemberResponse> members = family.getMembers() == null ? List.of() :
                family.getMembers().stream()
                        .map(this::toMemberResponse)
                        .toList();

        return FamilyResponse.builder()
                .id(family.getId())
                .name(family.getName())
                .description(family.getDescription())
                .familyCode(family.getFamilyCode())
                .currentMilestone(family.getCurrentMilestone())
                .municipio(family.getMunicipio())
                .whatsapp(family.getWhatsapp())
                .sentinelActive(family.getSentinelActive())
                .members(members)
                .build();
    }

    private FamilyMemberResponse toMemberResponse(FamilyMember member) {
        return FamilyMemberResponse.builder()
                .id(member.getId())
                .fullName(member.getFullName())
                .email(member.getEmail())
                .role(member.getRole())
                .age(member.getAge())
                .active(member.isActive())
                .autonomyLevel(member.getAutonomyLevel())
                .responsibilityLevel(member.getResponsibilityLevel())
                .build();
    }
}


