package com.integrityfamily.vitality.service;

import com.integrityfamily.common.exception.BusinessException;
import com.integrityfamily.domain.FamilyMember;
import com.integrityfamily.domain.repository.MemberRepository;
import com.integrityfamily.vitality.domain.DailyVitalityLog;
import com.integrityfamily.vitality.dto.VitalityDtos.RegisterLogRequest;
import com.integrityfamily.vitality.repository.DailyVitalityLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Registro y consulta de DailyVitalityLog (ADR-009, Fase 4 — base biológica).
 */
@Service
@RequiredArgsConstructor
public class VitalityService {

    private final DailyVitalityLogRepository repository;
    private final MemberRepository memberRepository;

    /**
     * Upsert por family_member_id + log_date. Solo sobreescribe los campos
     * presentes en el request — permite completar el panel en varias
     * llamadas durante el mismo día sin perder lo ya registrado.
     */
    @Transactional
    public DailyVitalityLog registerLog(Long familyId, Long familyMemberId, RegisterLogRequest request) {
        FamilyMember member = validateMembership(familyId, familyMemberId);

        LocalDate logDate = request.logDate() != null ? request.logDate() : LocalDate.now();

        DailyVitalityLog log = repository.findByFamilyMemberIdAndLogDate(familyMemberId, logDate)
                .orElseGet(() -> DailyVitalityLog.builder()
                        .familyMember(member)
                        .logDate(logDate)
                        .build());

        if (request.sleepHours() != null) log.setSleepHours(request.sleepHours());
        if (request.sleepQuality() != null) log.setSleepQuality(request.sleepQuality());
        if (request.exerciseMinutes() != null) log.setExerciseMinutes(request.exerciseMinutes());
        if (request.nutritionQuality() != null) log.setNutritionQuality(request.nutritionQuality());
        if (request.screenTimeBeforeBedMinutes() != null) log.setScreenTimeBeforeBedMinutes(request.screenTimeBeforeBedMinutes());
        if (request.fatigueLevel() != null) log.setFatigueLevel(request.fatigueLevel());

        return repository.save(log);
    }

    @Transactional(readOnly = true)
    public List<DailyVitalityLog> listByMember(Long familyId, Long familyMemberId, LocalDate from, LocalDate to) {
        validateMembership(familyId, familyMemberId);
        return repository.findByFamilyMemberIdAndLogDateBetweenOrderByLogDateAsc(familyMemberId, from, to);
    }

    private FamilyMember validateMembership(Long familyId, Long familyMemberId) {
        FamilyMember member = memberRepository.findById(familyMemberId)
                .orElseThrow(() -> new BusinessException("Miembro no encontrado: " + familyMemberId));
        if (member.getFamily() == null || !member.getFamily().getId().equals(familyId)) {
            throw new BusinessException("El miembro no pertenece a esta familia");
        }
        return member;
    }
}
