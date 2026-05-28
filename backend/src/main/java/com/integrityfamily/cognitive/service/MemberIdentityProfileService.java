package com.integrityfamily.cognitive.service;

import com.integrityfamily.domain.MemberIdentityProfile;
import com.integrityfamily.domain.repository.MemberIdentityProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Administra el perfil de identidad conversacional de cada miembro.
 * El perfil se crea vacío en el primer acceso y se enriquece progresivamente
 * a medida que la IA detecta patrones en las interacciones.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MemberIdentityProfileService {

    private final MemberIdentityProfileRepository repository;

    /** Obtiene el perfil del miembro o crea uno con valores por defecto. */
    @Transactional
    public MemberIdentityProfile getOrCreate(Long memberId) {
        return repository.findByMemberId(memberId).orElseGet(() -> {
            log.info("[IDENTITY-PROFILE] Creando perfil de identidad para miembro ID: {}", memberId);
            return repository.save(MemberIdentityProfile.builder()
                    .memberId(memberId)
                    .build());
        });
    }

    /** Actualiza campos de estilo conversacional inferidos por el motor IA. */
    @Transactional
    public MemberIdentityProfile update(
            Long memberId,
            String communicationStyle,
            Integer reflexivityLevel,
            Integer emotionalSensitivity,
            String changeResistance,
            String evasionPatternsJson,
            String motivatorsJson) {

        MemberIdentityProfile profile = getOrCreate(memberId);

        if (communicationStyle != null)   profile.setCommunicationStyle(communicationStyle);
        if (reflexivityLevel != null)     profile.setReflexivityLevel(reflexivityLevel);
        if (emotionalSensitivity != null) profile.setEmotionalSensitivity(emotionalSensitivity);
        if (changeResistance != null)     profile.setChangeResistance(changeResistance);
        if (evasionPatternsJson != null)  profile.setEvasionPatterns(evasionPatternsJson);
        if (motivatorsJson != null)       profile.setMotivators(motivatorsJson);

        return repository.save(profile);
    }
}
