package com.integrityfamily.member.service;

import com.integrityfamily.common.exception.BusinessException;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.FamilyMember;
import com.integrityfamily.domain.Role;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.member.dto.MemberRequest;
import com.integrityfamily.domain.repository.MemberRepository;
import com.integrityfamily.common.config.RabbitConfig;
import com.integrityfamily.common.event.SystemEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {


    private final MemberRepository memberRepository;
    private final FamilyRepository familyRepository;
    private final RabbitTemplate rabbitTemplate;

    public List<FamilyMember> findAll() {
        return memberRepository.findAll();
    }

    public List<FamilyMember> findByFamily(Long familyId) {
        return memberRepository.findByFamilyId(familyId);
    }

    public FamilyMember findById(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Miembro no encontrado", "MEMBER_NOT_FOUND", HttpStatus.NOT_FOUND));
    }

    @Transactional
    public FamilyMember createMember(MemberRequest req) {
        Long familyId = req.familyId();
        log.info("[MEMBER-SERVICE] Registrando miembro: {} en familia {}", req.fullName(), familyId);

        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new BusinessException("Familia no encontrada: " + familyId, "FAMILY_NOT_FOUND", org.springframework.http.HttpStatus.NOT_FOUND));

        // SDD-MAPPING: Transferencia de datos del Record a la Entidad
        FamilyMember member = new FamilyMember();
        member.setFullName(req.fullName());
        
        // Derivación de First Name si no se provee (Protocolo Sentinel)
        if (req.fullName().contains(" ")) {
            member.setFirstName(req.fullName().split(" ")[0]);
        } else {
            member.setFirstName(req.fullName());
        }

        member.setRole(req.roleType());
        member.setAge(req.age() != null ? req.age() : 0);
        member.setEmail(req.email());
        member.setPhone(req.phone());
        
        // Niveles de Desarrollo (Valores por defecto de 50 si son nulos)

        member.setAutonomyLevel(req.autonomyLevel() != null ? req.autonomyLevel() : 50);
        member.setResponsibilityLevel(req.responsibilityLevel() != null ? req.responsibilityLevel() : 50);

        member.setJoinedAt(java.time.LocalDateTime.now());
        member.setActive(true);
        member.setFamily(family);

        FamilyMember saved = memberRepository.save(member);
        
        // SDD-EVENT: Disparo de evento 'members.updated'
        try {
            rabbitTemplate.convertAndSend(
                RabbitConfig.EXCHANGE_NAME, 
                "members.updated", 
                SystemEvent.of("members.updated", familyId, saved.getFullName(), "SYSTEM")
            );
            log.info("[MEMBER-EVENT] Evento members.updated enviado para: {}", saved.getFullName());
        } catch (Exception e) {
            log.error("[MEMBER-EVENT] Error al enviar evento: {}", e.getMessage());
        }

        return saved;
    }


    @Transactional
    public FamilyMember update(Long id, FamilyMember request) {
        FamilyMember existing = findById(id);
        existing.setFullName(request.getFullName());
        existing.setRole(request.getRole());
        existing.setActive(request.isActive());
        return memberRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        memberRepository.deleteById(id);
    }
}
