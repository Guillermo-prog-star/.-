package com.integrityfamily.lineage.service;

import com.integrityfamily.common.exception.BusinessException;
import com.integrityfamily.domain.Family;
import com.integrityfamily.lineage.domain.*;
import com.integrityfamily.lineage.dto.*;
import com.integrityfamily.lineage.repository.*;
import com.integrityfamily.domain.repository.FamilyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LineageService {

    private final FamilyLineageRepository        lineageRepo;
    private final LineageMemberRepository        memberRepo;
    private final LineageRelationshipRepository  relRepo;
    private final LineageGenerationInfoRepository genInfoRepo;
    private final LineageEventRepository         eventRepo;
    private final FamilyRepository               familyRepo;

    // ── LINEAGE ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public LineageResponse getByFamily(Long familyId) {
        FamilyLineage lineage = lineageRepo.findByFamilyId(familyId)
                .orElseThrow(() -> new BusinessException(
                        "Linaje no encontrado", "LINEAGE_NOT_FOUND", HttpStatus.NOT_FOUND));
        return toResponse(lineage, true);
    }

    @Transactional
    public LineageResponse create(Long familyId, CreateLineageRequest req) {
        if (lineageRepo.existsByFamilyId(familyId)) {
            throw new BusinessException(
                    "La familia ya tiene un linaje registrado", "LINEAGE_EXISTS", HttpStatus.CONFLICT);
        }
        Family family = familyRepo.findById(familyId)
                .orElseThrow(() -> new BusinessException(
                        "Familia no encontrada", "FAMILY_NOT_FOUND", HttpStatus.NOT_FOUND));

        String safeName = family.getName().toUpperCase().replaceAll("[^A-Z0-9]", "");
        String code = "IF-LIN-" + safeName.substring(0, Math.min(6, safeName.length()))
                + "-" + String.format("%04d", familyId);

        FamilyLineage lineage = FamilyLineage.builder()
                .family(family)
                .lineageCode(code)
                .title(req.title())
                .description(req.description())
                .anchorGeneration(req.anchorGeneration() != null ? req.anchorGeneration() : 0)
                .maxPastGen(req.maxPastGen() != null ? req.maxPastGen() : -2)
                .maxFutureGen(req.maxFutureGen() != null ? req.maxFutureGen() : 2)
                .visionStatement(req.visionStatement())
                .foundingYear(req.foundingYear())
                .build();

        return toResponse(lineageRepo.save(lineage), false);
    }

    @Transactional
    public LineageResponse update(Long familyId, CreateLineageRequest req) {
        FamilyLineage lineage = getLineageEntity(familyId);
        lineage.setTitle(req.title());
        lineage.setDescription(req.description());
        if (req.anchorGeneration() != null) lineage.setAnchorGeneration(req.anchorGeneration());
        if (req.maxPastGen()       != null) lineage.setMaxPastGen(req.maxPastGen());
        if (req.maxFutureGen()     != null) lineage.setMaxFutureGen(req.maxFutureGen());
        if (req.visionStatement()  != null) lineage.setVisionStatement(req.visionStatement());
        if (req.foundingYear()     != null) lineage.setFoundingYear(req.foundingYear());
        return toResponse(lineageRepo.save(lineage), true);
    }

    // ── MEMBERS ────────────────────────────────────────────────────────────

    @Transactional
    public LineageMemberResponse addMember(Long familyId, LineageMemberRequest req) {
        FamilyLineage lineage = getLineageEntity(familyId);
        LineageMember member = buildMember(req, lineage);
        return toMemberResponse(memberRepo.save(member));
    }

    @Transactional
    public LineageMemberResponse updateMember(Long familyId, Long memberId, LineageMemberRequest req) {
        LineageMember member = getMemberEntity(familyId, memberId);
        applyMemberFields(req, member);
        // Solo actualizar eventos si se envía la lista explícitamente (null = no tocar)
        if (req.events() != null) {
            member.getEvents().clear();
            req.events().forEach(e -> member.getEvents().add(buildEvent(e, member)));
        }
        return toMemberResponse(memberRepo.save(member));
    }

    @Transactional
    public void deleteMember(Long familyId, Long memberId) {
        LineageMember member = getMemberEntity(familyId, memberId);
        relRepo.findByLineageId(member.getLineage().getId()).stream()
                .filter(r -> r.getFromMember().getId().equals(memberId)
                          || r.getToMember().getId().equals(memberId))
                .forEach(relRepo::delete);
        memberRepo.delete(member);
    }

    // ── RELATIONSHIPS ──────────────────────────────────────────────────────

    @Transactional
    public LineageRelationshipResponse addRelationship(Long familyId, LineageRelationshipRequest req) {
        FamilyLineage lineage = getLineageEntity(familyId);
        LineageMember from = memberRepo.findById(req.fromMemberId())
                .orElseThrow(() -> new BusinessException(
                        "Miembro origen no encontrado", "MEMBER_NOT_FOUND", HttpStatus.NOT_FOUND));
        LineageMember to = memberRepo.findById(req.toMemberId())
                .orElseThrow(() -> new BusinessException(
                        "Miembro destino no encontrado", "MEMBER_NOT_FOUND", HttpStatus.NOT_FOUND));

        LineageRelationship rel = LineageRelationship.builder()
                .lineage(lineage)
                .fromMember(from)
                .toMember(to)
                .relationshipType(req.relationshipType() != null ? req.relationshipType() : "biological")
                .isCouple(Boolean.TRUE.equals(req.isCouple()))
                .build();

        return toRelResponse(relRepo.save(rel));
    }

    @Transactional
    public void deleteRelationship(Long familyId, Long relId) {
        LineageRelationship rel = relRepo.findById(relId)
                .orElseThrow(() -> new BusinessException(
                        "Relación no encontrada", "REL_NOT_FOUND", HttpStatus.NOT_FOUND));
        relRepo.delete(rel);
    }

    // ── GENERATION INFO ────────────────────────────────────────────────────

    @Transactional
    public GenerationInfoResponse upsertGenerationInfo(Long familyId, GenerationInfoRequest req) {
        FamilyLineage lineage = getLineageEntity(familyId);
        LineageGenerationInfo info = genInfoRepo
                .findByLineageIdAndGenerationLevel(lineage.getId(), req.generationLevel())
                .orElseGet(() -> LineageGenerationInfo.builder()
                        .lineage(lineage)
                        .generationLevel(req.generationLevel())
                        .build());

        if (req.generationType()  != null) info.setGenerationType(req.generationType());
        if (req.title()           != null) info.setTitle(req.title());
        if (req.summary()         != null) info.setSummary(req.summary());
        if (req.context()         != null) info.setContext(req.context());
        if (req.keyChallenge()    != null) info.setKeyChallenge(req.keyChallenge());
        if (req.keyAchievement()  != null) info.setKeyAchievement(req.keyAchievement());
        if (req.periodStart()     != null) info.setPeriodStart(req.periodStart());
        if (req.periodEnd()       != null) info.setPeriodEnd(req.periodEnd());

        return toGenInfoResponse(genInfoRepo.save(info));
    }

    // ── EVENTS ────────────────────────────────────────────────────────────────

    @Transactional
    public LineageEventResponse addEvent(Long familyId, Long memberId, LineageEventRequest req) {
        LineageMember member = getMemberEntity(familyId, memberId);
        LineageEvent event = buildEvent(req, member);
        return toEventResponse(eventRepo.save(event));
    }

    @Transactional
    public LineageEventResponse updateEvent(Long familyId, Long memberId, Long eventId, LineageEventRequest req) {
        getMemberEntity(familyId, memberId);
        LineageEvent event = eventRepo.findById(eventId)
                .orElseThrow(() -> new BusinessException(
                        "Evento no encontrado", "EVENT_NOT_FOUND", HttpStatus.NOT_FOUND));
        if (req.eventYear()     != null) event.setEventYear(req.eventYear());
        if (req.title()         != null) event.setTitle(req.title());
        if (req.description()   != null) event.setDescription(req.description());
        if (req.eventType()     != null) event.setEventType(req.eventType());
        if (req.isApproximate() != null) event.setIsApproximate(req.isApproximate());
        if (req.sortOrder()     != null) event.setSortOrder(req.sortOrder());
        return toEventResponse(eventRepo.save(event));
    }

    @Transactional
    public void deleteEvent(Long familyId, Long memberId, Long eventId) {
        getMemberEntity(familyId, memberId);
        LineageEvent event = eventRepo.findById(eventId)
                .orElseThrow(() -> new BusinessException(
                        "Evento no encontrado", "EVENT_NOT_FOUND", HttpStatus.NOT_FOUND));
        eventRepo.delete(event);
    }

    // ── PRIVATE HELPERS ────────────────────────────────────────────────────

    private FamilyLineage getLineageEntity(Long familyId) {
        return lineageRepo.findByFamilyId(familyId)
                .orElseThrow(() -> new BusinessException(
                        "Linaje no encontrado", "LINEAGE_NOT_FOUND", HttpStatus.NOT_FOUND));
    }

    private LineageMember getMemberEntity(Long familyId, Long memberId) {
        LineageMember member = memberRepo.findById(memberId)
                .orElseThrow(() -> new BusinessException(
                        "Miembro no encontrado", "MEMBER_NOT_FOUND", HttpStatus.NOT_FOUND));
        if (!member.getLineage().getFamily().getId().equals(familyId)) {
            throw new BusinessException("Acceso denegado", "ACCESS_DENIED", HttpStatus.FORBIDDEN);
        }
        return member;
    }

    private LineageMember buildMember(LineageMemberRequest req, FamilyLineage lineage) {
        LineageMember m = new LineageMember();
        m.setLineage(lineage);
        applyMemberFields(req, m);
        if (req.events() != null) {
            req.events().forEach(e -> m.getEvents().add(buildEvent(e, m)));
        }
        return m;
    }

    private void applyMemberFields(LineageMemberRequest req, LineageMember m) {
        m.setFirstName(req.firstName());
        m.setLastName(req.lastName());
        m.setAvatarInitials(req.avatarInitials());
        m.setAvatarColor(req.avatarColor());
        m.setGeneration(req.generation());
        m.setGenerationType(req.generationType());
        m.setIsAnchor(Boolean.TRUE.equals(req.isAnchor()));
        m.setStatus(req.status() != null ? req.status() : "unknown");
        m.setBirthYear(req.birthYear());
        m.setBirthYearApproximate(req.birthYearApproximate());
        m.setBirthDate(parseDate(req.birthDate()));
        m.setDeathYear(req.deathYear());
        m.setDeathDate(parseDate(req.deathDate()));
        m.setOrigin(req.origin());
        m.setRoleLabel(req.roleLabel());
        m.setConfidenceLevel(req.confidenceLevel() != null ? req.confidenceLevel() : 50);
        m.setDataSource(req.dataSource());
        m.setStory(req.story());
        m.setValores(req.valores());
        m.setAprendizajes(req.aprendizajes());
        m.setErroresSuperados(req.erroresSuperados());
        m.setTradiciones(req.tradiciones());
        m.setMisionesCumplidas(req.misionesCumplidas());
        m.setLegadoPersonal(req.legadoPersonal());
        m.setPhotoUrl(req.photoUrl());
        m.setPositionX(req.positionX());
        m.setPositionY(req.positionY());
        m.setFamilyMemberId(req.familyMemberId());
    }

    private LineageEvent buildEvent(LineageEventRequest req, LineageMember member) {
        return LineageEvent.builder()
                .member(member)
                .eventYear(req.eventYear())
                .title(req.title())
                .description(req.description())
                .eventType(req.eventType() != null ? req.eventType() : "milestone")
                .isApproximate(Boolean.TRUE.equals(req.isApproximate()))
                .sortOrder(req.sortOrder() != null ? req.sortOrder() : 0)
                .build();
    }

    private LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        try { return LocalDate.parse(s); } catch (DateTimeParseException e) { return null; }
    }

    // ── MAPPERS ────────────────────────────────────────────────────────────

    private LineageResponse toResponse(FamilyLineage l, boolean includeDetails) {
        List<LineageMemberResponse> members = includeDetails
                ? memberRepo.findWithEventsByLineageId(l.getId()).stream()
                    .map(this::toMemberResponse).collect(Collectors.toList())
                : List.of();
        List<LineageRelationshipResponse> rels = includeDetails
                ? relRepo.findByLineageId(l.getId()).stream()
                    .map(this::toRelResponse).collect(Collectors.toList())
                : List.of();
        List<GenerationInfoResponse> genInfos = includeDetails
                ? genInfoRepo.findByLineageIdOrderByGenerationLevel(l.getId()).stream()
                    .map(this::toGenInfoResponse).collect(Collectors.toList())
                : List.of();

        return new LineageResponse(
                l.getId(), l.getFamily().getId(), l.getLineageCode(),
                l.getTitle(), l.getDescription(),
                l.getAnchorGeneration(), l.getMaxPastGen(), l.getMaxFutureGen(),
                l.getVisionStatement(), l.getFoundingYear(),
                members, rels, genInfos);
    }

    private LineageMemberResponse toMemberResponse(LineageMember m) {
        List<LineageEventResponse> events = m.getEvents() != null
                ? m.getEvents().stream().map(this::toEventResponse).collect(Collectors.toList())
                : List.of();
        return new LineageMemberResponse(
                m.getId(), m.getFirstName(), m.getLastName(), m.getFullName(),
                m.getAvatarInitials(), m.getAvatarColor(),
                m.getGeneration(), m.resolvedGenerationType(), m.getIsAnchor(),
                m.getStatus(),
                m.getBirthYear(), m.getBirthYearApproximate(),
                m.getBirthDate()  != null ? m.getBirthDate().toString()  : null,
                m.getDeathYear(),
                m.getDeathDate()  != null ? m.getDeathDate().toString()  : null,
                m.getOrigin(), m.getRoleLabel(), m.getConfidenceLevel(), m.getDataSource(),
                m.getCalculatedAge(),
                m.getStory(), m.getValores(), m.getAprendizajes(),
                m.getErroresSuperados(), m.getTradiciones(),
                m.getMisionesCumplidas(), m.getLegadoPersonal(),
                m.getPhotoUrl(), m.getPositionX(), m.getPositionY(), m.getFamilyMemberId(), events);
    }

    private LineageEventResponse toEventResponse(LineageEvent e) {
        return new LineageEventResponse(e.getId(), e.getEventYear(), e.getTitle(),
                e.getDescription(), e.getEventType(), e.getIsApproximate(), e.getSortOrder());
    }

    private LineageRelationshipResponse toRelResponse(LineageRelationship r) {
        return new LineageRelationshipResponse(r.getId(),
                r.getFromMember().getId(), r.getToMember().getId(),
                r.getRelationshipType(), r.getIsCouple());
    }

    private GenerationInfoResponse toGenInfoResponse(LineageGenerationInfo g) {
        return new GenerationInfoResponse(
                g.getId(), g.getGenerationLevel(), g.getGenerationType(),
                g.getTitle(), g.getSummary(), g.getContext(),
                g.getKeyChallenge(), g.getKeyAchievement(),
                g.getPeriodStart(), g.getPeriodEnd());
    }
}
