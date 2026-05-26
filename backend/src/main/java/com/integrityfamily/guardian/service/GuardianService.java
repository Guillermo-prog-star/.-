package com.integrityfamily.guardian.service;

import com.integrityfamily.common.exception.BusinessException;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.FamilyMember;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.repository.MemberRepository;
import com.integrityfamily.guardian.domain.FamilyMission;
import com.integrityfamily.guardian.domain.GuardianVote;
import com.integrityfamily.guardian.domain.MissionStatus;
import com.integrityfamily.guardian.dto.*;
import com.integrityfamily.guardian.repository.FamilyMissionRepository;
import com.integrityfamily.guardian.repository.GuardianVoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * GuardianService — Motor del Guardián Familiar.
 *
 * Reglas de negocio:
 * - Cualquier miembro puede votar por cualquier otro (o por sí mismo).
 * - Un miembro solo puede emitir un voto. Puede cambiarlo.
 * - El Guardián se confirma cuando la mayoría vota por el mismo miembro.
 * - El Guardián puede activar y completar misiones familiares.
 * - El Guardián NO accede a datos privados de otros miembros.
 */
@Service
@RequiredArgsConstructor
public class GuardianService {

    private final FamilyRepository familyRepository;
    private final MemberRepository memberRepository;
    private final GuardianVoteRepository voteRepository;
    private final FamilyMissionRepository missionRepository;

    // ─────────────────────────────────────────────
    // Estado del Guardián
    // ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public GuardianStatusResponse getStatus(Long familyId, Long currentMemberId) {
        Family family = getFamily(familyId);

        List<Object[]> voteCounts = voteRepository.countVotesByFamilyGroupedByNominated(familyId);
        long totalVotes = voteRepository.countByFamilyId(familyId);
        boolean currentUserVoted = currentMemberId != null &&
            voteRepository.existsByFamilyIdAndVoterMemberId(familyId, currentMemberId);

        List<GuardianStatusResponse.VoteCount> counts = voteCounts.stream()
            .map(row -> {
                Long memberId = (Long) row[0];
                int votes = ((Long) row[1]).intValue();
                String name = memberRepository.findById(memberId)
                    .map(FamilyMember::getFullName).orElse("Desconocido");
                return GuardianStatusResponse.VoteCount.builder()
                    .memberId(memberId).fullName(name).votes(votes).build();
            }).toList();

        // Misión activa
        MissionDto activeMission = missionRepository
            .findTopByFamilyIdAndStatusOrderByActivatedAtDesc(familyId, MissionStatus.ACTIVE)
            .map(this::toMissionDto).orElse(null);

        long completed = missionRepository.countByFamilyIdAndStatus(familyId, MissionStatus.COMPLETED);

        // Nombre del guardián actual
        String guardianName = null;
        if (family.getGuardianMemberId() != null) {
            guardianName = memberRepository.findById(family.getGuardianMemberId())
                .map(FamilyMember::getFullName).orElse("Guardián");
        }

        return GuardianStatusResponse.builder()
            .familyId(familyId)
            .hasGuardian(family.getGuardianMemberId() != null)
            .guardianMemberId(family.getGuardianMemberId())
            .guardianFullName(guardianName)
            .guardianSince(family.getGuardianSince())
            .totalVotes((int) totalVotes)
            .voteCounts(counts)
            .currentUserHasVoted(currentUserVoted)
            .activeMission(activeMission)
            .participationScore(family.getParticipationScore() != null ? family.getParticipationScore() : 0)
            .completedMissions((int) completed)
            .build();
    }

    // ─────────────────────────────────────────────
    // Votación
    // ─────────────────────────────────────────────

    @Transactional
    public GuardianStatusResponse vote(Long familyId, VoteRequest request) {
        Family family = getFamily(familyId);
        FamilyMember voter     = getMember(request.voterMemberId(), familyId);
        FamilyMember nominated = getMember(request.nominatedMemberId(), familyId);

        // Upsert: si ya votó, cambia el voto
        GuardianVote vote = voteRepository
            .findByFamilyIdAndVoterMemberId(familyId, voter.getId())
            .orElseGet(() -> GuardianVote.builder()
                .family(family).voter(voter).build());
        vote.setNominated(nominated);
        voteRepository.save(vote);

        // Resolver guardián si hay mayoría
        resolveGuardianIfMajority(family);
        return getStatus(familyId, voter.getId());
    }

    /** Confirmar manualmente al Guardián (por acuerdo unánime o invitación directa). */
    @Transactional
    public GuardianStatusResponse confirmGuardian(Long familyId, Long memberId) {
        Family family = getFamily(familyId);
        FamilyMember member = getMember(memberId, familyId);
        family.setGuardianMemberId(member.getId());
        family.setGuardianSince(LocalDateTime.now());
        familyRepository.save(family);
        return getStatus(familyId, memberId);
    }

    // ─────────────────────────────────────────────
    // Misiones
    // ─────────────────────────────────────────────

    @Transactional
    public MissionDto activateMission(Long familyId, ActivateMissionRequest req) {
        Family family = getFamily(familyId);

        // Solo el Guardián puede activar misiones
        if (family.getGuardianMemberId() == null ||
            !family.getGuardianMemberId().equals(req.guardianMemberId())) {
            throw new BusinessException(
                "Solo el Guardián Familiar puede activar misiones",
                "NOT_GUARDIAN", HttpStatus.FORBIDDEN);
        }

        // Cancelar misión activa anterior si existe
        missionRepository.findTopByFamilyIdAndStatusOrderByActivatedAtDesc(familyId, MissionStatus.ACTIVE)
            .ifPresent(m -> { m.setStatus(MissionStatus.CANCELLED); missionRepository.save(m); });

        FamilyMember guardian = getMember(req.guardianMemberId(), familyId);
        FamilyMission mission = FamilyMission.builder()
            .family(family)
            .title(req.title())
            .description(req.description())
            .category(req.category())
            .durationMinutes(req.durationMinutes() != null ? req.durationMinutes() : 60)
            .status(MissionStatus.ACTIVE)
            .createdBy(guardian)
            .activatedAt(LocalDateTime.now())
            .build();

        return toMissionDto(missionRepository.save(mission));
    }

    @Transactional
    public MissionDto completeMission(Long familyId, Long missionId, Long guardianMemberId) {
        Family family = getFamily(familyId);
        FamilyMission mission = missionRepository.findById(missionId)
            .orElseThrow(() -> new BusinessException("Misión no encontrada", "MISSION_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (!mission.getFamily().getId().equals(familyId))
            throw new BusinessException("La misión no pertenece a esta familia", "MISSION_MISMATCH", HttpStatus.BAD_REQUEST);

        mission.setStatus(MissionStatus.COMPLETED);
        mission.setCompletedAt(LocalDateTime.now());

        // Incrementar puntuación de participación
        int score = family.getParticipationScore() != null ? family.getParticipationScore() : 0;
        family.setParticipationScore(score + 10);
        familyRepository.save(family);

        return toMissionDto(missionRepository.save(mission));
    }

    @Transactional(readOnly = true)
    public List<MissionDto> getMissions(Long familyId) {
        return missionRepository.findByFamilyIdOrderByCreatedAtDesc(familyId)
            .stream().map(this::toMissionDto).toList();
    }

    // ─────────────────────────────────────────────
    // Helpers privados
    // ─────────────────────────────────────────────

    private void resolveGuardianIfMajority(Family family) {
        List<Object[]> votes = voteRepository.countVotesByFamilyGroupedByNominated(family.getId());
        if (votes.isEmpty()) return;

        long totalMembers = family.getMembers() != null ? family.getMembers().size() : 1;
        Object[] top = votes.get(0);
        long topVotes = (Long) top[1];

        // Mayoría simple: más de la mitad
        if (topVotes > totalMembers / 2.0) {
            family.setGuardianMemberId((Long) top[0]);
            if (family.getGuardianSince() == null) {
                family.setGuardianSince(LocalDateTime.now());
            }
            familyRepository.save(family);
        }
    }

    private Family getFamily(Long id) {
        return familyRepository.findById(id)
            .orElseThrow(() -> new BusinessException("Familia no encontrada", "FAMILY_NOT_FOUND", HttpStatus.NOT_FOUND));
    }

    private FamilyMember getMember(Long memberId, Long familyId) {
        FamilyMember m = memberRepository.findById(memberId)
            .orElseThrow(() -> new BusinessException("Miembro no encontrado", "MEMBER_NOT_FOUND", HttpStatus.NOT_FOUND));
        if (!m.getFamily().getId().equals(familyId))
            throw new BusinessException("El miembro no pertenece a esta familia", "MEMBER_MISMATCH", HttpStatus.BAD_REQUEST);
        return m;
    }

    public MissionDto toMissionDto(FamilyMission m) {
        return MissionDto.builder()
            .id(m.getId())
            .title(m.getTitle())
            .description(m.getDescription())
            .category(m.getCategory())
            .durationMinutes(m.getDurationMinutes())
            .status(m.getStatus())
            .createdByMemberId(m.getCreatedBy() != null ? m.getCreatedBy().getId() : null)
            .createdByFullName(m.getCreatedBy() != null ? m.getCreatedBy().getFullName() : null)
            .activatedAt(m.getActivatedAt())
            .completedAt(m.getCompletedAt())
            .createdAt(m.getCreatedAt())
            .build();
    }
}
