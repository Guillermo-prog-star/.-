package com.integrityfamily.trajectory.service;

import com.integrityfamily.common.service.UserNotificationService;
import com.integrityfamily.domain.FamilyMember;
import com.integrityfamily.domain.FamilyRiskTrajectory;
import com.integrityfamily.domain.SafetyProtocolActivation;
import com.integrityfamily.domain.repository.FamilyRiskTrajectoryRepository;
import com.integrityfamily.domain.repository.MemberRepository;
import com.integrityfamily.domain.repository.SafetyProtocolActivationRepository;
import com.integrityfamily.trajectory.dto.TrajectoryDtos.ActivateSafetyProtocolRequest;
import com.integrityfamily.trajectory.dto.TrajectoryDtos.CloseSafetyProtocolRequest;
import com.integrityfamily.trajectory.dto.TrajectoryDtos.SafetyProtocolDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Activación y seguimiento del protocolo de seguridad para trayectorias con
 * requires_safety_protocol=true. Siempre activado por una persona (Guardián o
 * profesional) que confirma responsable, acción inicial y fecha de seguimiento
 * — nunca se dispara solo en segundo plano.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SafetyProtocolService {

    private final SafetyProtocolActivationRepository repo;
    private final FamilyRiskTrajectoryRepository familyTrajectoryRepo;
    private final MemberRepository memberRepo;
    private final UserNotificationService notificationService;

    @Transactional
    public SafetyProtocolDto activate(Long familyTrajectoryId, ActivateSafetyProtocolRequest req, String activatedBy) {
        FamilyRiskTrajectory frt = familyTrajectoryRepo.findById(familyTrajectoryId)
            .orElseThrow(() -> new IllegalArgumentException("Trayectoria familiar no encontrada: " + familyTrajectoryId));

        if (req.responsibleId() == null)
            throw new IllegalArgumentException("El protocolo de seguridad requiere un responsable asignado");
        if (req.initialAction() == null || req.initialAction().isBlank())
            throw new IllegalArgumentException("El protocolo de seguridad requiere una acción inicial");
        if (req.followUpDate() == null)
            throw new IllegalArgumentException("El protocolo de seguridad requiere fecha de seguimiento");

        FamilyMember responsible = memberRepo.findById(req.responsibleId())
            .orElseThrow(() -> new IllegalArgumentException("Miembro responsable no encontrado: " + req.responsibleId()));
        if (responsible.getFamily() == null || !responsible.getFamily().getId().equals(frt.getFamily().getId())) {
            throw new IllegalArgumentException("El responsable no pertenece a esta familia");
        }

        SafetyProtocolActivation activation = SafetyProtocolActivation.builder()
            .familyTrajectory(frt)
            .responsible(responsible)
            .initialAction(req.initialAction())
            .followUpDate(req.followUpDate())
            .activatedBy(activatedBy)
            .build();

        SafetyProtocolActivation saved = repo.save(activation);
        log.info("[SAFETY-PROTOCOL] Activado para trayectoria familiar {} — responsable={}, seguimiento={}",
            familyTrajectoryId, responsible.getId(), req.followUpDate());

        notificationService.push(frt.getFamily(), null, "SAFETY_PROTOCOL_ACTIVATED",
            "🔴 Protocolo de seguridad activado",
            "Se activó un protocolo de seguridad para \"" + frt.getTrajectory().getName() + "\". "
            + "Responsable: " + responsible.getFullName() + ". "
            + "Seguimiento: " + req.followUpDate() + ".");

        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<SafetyProtocolDto> getByFamilyTrajectory(Long familyTrajectoryId) {
        return repo.findByFamilyTrajectoryIdOrderByCreatedAtDesc(familyTrajectoryId)
            .stream().map(this::toDto).toList();
    }

    @Transactional
    public SafetyProtocolDto close(Long familyTrajectoryId, Long activationId, CloseSafetyProtocolRequest req) {
        SafetyProtocolActivation activation = repo.findById(activationId)
            .orElseThrow(() -> new IllegalArgumentException("Activación no encontrada: " + activationId));
        if (!activation.getFamilyTrajectory().getId().equals(familyTrajectoryId)) {
            throw new IllegalArgumentException("La activación no pertenece a esta trayectoria");
        }
        if (req.resolutionNotes() == null || req.resolutionNotes().isBlank()) {
            throw new IllegalArgumentException("El cierre del protocolo de seguridad requiere notas de resolución");
        }

        activation.setClosed(true);
        activation.setResolutionNotes(req.resolutionNotes());
        activation.setClosedAt(LocalDateTime.now());
        return toDto(repo.save(activation));
    }

    private SafetyProtocolDto toDto(SafetyProtocolActivation a) {
        return new SafetyProtocolDto(
            a.getId(),
            a.getFamilyTrajectory().getId(),
            a.getResponsible().getId(),
            a.getResponsible().getFullName(),
            a.getInitialAction(),
            a.getFollowUpDate(),
            a.getSupportAssignment() != null ? a.getSupportAssignment().getId() : null,
            Boolean.TRUE.equals(a.getClosed()),
            a.getResolutionNotes(),
            a.getActivatedBy(),
            a.getCreatedAt(),
            a.getClosedAt()
        );
    }
}
