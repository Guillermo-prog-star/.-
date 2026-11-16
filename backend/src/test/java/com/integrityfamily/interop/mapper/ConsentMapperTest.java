package com.integrityfamily.interop.mapper;

import com.integrityfamily.consent.domain.ConsentPurpose;
import com.integrityfamily.consent.domain.ConsentStatus;
import com.integrityfamily.interop.canonical.Consent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ConsentMapper")
class ConsentMapperTest {

    @Test
    @DisplayName("con memberId → subjectId apunta al miembro, no a la familia")
    void shouldUseMemberAsSubject_whenPresent() {
        com.integrityfamily.consent.domain.Consent consent = com.integrityfamily.consent.domain.Consent.builder()
                .id(1L).familyId(10L).memberId(5L)
                .purpose(ConsentPurpose.HEALTH_INTEROPERABILITY)
                .scope("ICF_SCORE,RISK_LEVEL")
                .granteeReference("Ministerio de Salud")
                .status(ConsentStatus.GRANTED)
                .grantedAt(LocalDateTime.of(2026, 7, 1, 0, 0))
                .build();

        Consent canonical = ConsentMapper.toCanonical(consent);

        assertThat(canonical.canonicalId()).isEqualTo("consent-1");
        assertThat(canonical.subjectId()).isEqualTo("member-5");
        assertThat(canonical.grantedToId()).isEqualTo("Ministerio de Salud");
        assertThat(canonical.purpose()).isEqualTo("HEALTH_INTEROPERABILITY");
        assertThat(canonical.status()).isEqualTo("GRANTED");
    }

    @Test
    @DisplayName("sin memberId → subjectId apunta a la familia completa")
    void shouldUseFamilyAsSubject_whenMemberNull() {
        com.integrityfamily.consent.domain.Consent consent = com.integrityfamily.consent.domain.Consent.builder()
                .id(2L).familyId(10L).memberId(null)
                .purpose(ConsentPurpose.RESEARCH)
                .scope("EVALUATIONS")
                .granteeReference("Universidad X")
                .status(ConsentStatus.REVOKED)
                .grantedAt(LocalDateTime.now())
                .revokedAt(LocalDateTime.now())
                .build();

        Consent canonical = ConsentMapper.toCanonical(consent);

        assertThat(canonical.subjectId()).isEqualTo("family-10");
        assertThat(canonical.status()).isEqualTo("REVOKED");
        assertThat(canonical.revokedAt()).isNotNull();
    }
}
