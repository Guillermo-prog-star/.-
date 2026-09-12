package com.integrityfamily.consent.service;

import com.integrityfamily.auth.service.AuditService;
import com.integrityfamily.common.exception.BusinessException;
import com.integrityfamily.consent.domain.Consent;
import com.integrityfamily.consent.domain.ConsentPurpose;
import com.integrityfamily.consent.domain.ConsentStatus;
import com.integrityfamily.consent.dto.ConsentDtos.ConsentResponse;
import com.integrityfamily.consent.dto.ConsentDtos.GrantRequest;
import com.integrityfamily.consent.dto.ConsentDtos.RevokeRequest;
import com.integrityfamily.consent.repository.ConsentRepository;
import com.integrityfamily.domain.AuditEventType;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.FamilyMember;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConsentService — Unit Tests")
class ConsentServiceTest {

    @Mock ConsentRepository consentRepository;
    @Mock FamilyRepository familyRepository;
    @Mock MemberRepository memberRepository;
    @Mock AuditService auditService;

    @InjectMocks
    ConsentService consentService;

    private Family family;
    private FamilyMember member;

    @BeforeEach
    void setUp() {
        family = Family.builder().id(1L).name("Familia López").build();
        member = new FamilyMember();
        member.setId(10L);
        member.setFamily(family);
    }

    @Nested
    @DisplayName("grant()")
    class Grant {

        @Test
        @DisplayName("familia inexistente → BusinessException NOT_FOUND")
        void shouldThrow_whenFamilyNotFound() {
            when(familyRepository.existsById(99L)).thenReturn(false);

            GrantRequest req = new GrantRequest(null, ConsentPurpose.HEALTH_INTEROPERABILITY,
                    "ICF_SCORE", "Ministerio de Salud", null);

            assertThatThrownBy(() -> consentService.grant(99L, req, "ana@test.com"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getStatus())
                            .isEqualTo(org.springframework.http.HttpStatus.NOT_FOUND));
        }

        @Test
        @DisplayName("miembro de otra familia → BusinessException FORBIDDEN")
        void shouldThrow_whenMemberBelongsToAnotherFamily() {
            when(familyRepository.existsById(1L)).thenReturn(true);
            FamilyMember otherFamilyMember = new FamilyMember();
            otherFamilyMember.setId(20L);
            otherFamilyMember.setFamily(Family.builder().id(2L).build());
            when(memberRepository.findById(20L)).thenReturn(Optional.of(otherFamilyMember));

            GrantRequest req = new GrantRequest(20L, ConsentPurpose.RESEARCH, "EVALUATIONS", "Universidad X", null);

            assertThatThrownBy(() -> consentService.grant(1L, req, "ana@test.com"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getStatus())
                            .isEqualTo(org.springframework.http.HttpStatus.FORBIDDEN));
        }

        @Test
        @DisplayName("caso exitoso → guarda GRANTED, registra auditoría y devuelve activo")
        void shouldGrant_andAudit() {
            when(familyRepository.existsById(1L)).thenReturn(true);
            when(consentRepository.save(any(Consent.class))).thenAnswer(i -> {
                Consent c = i.getArgument(0);
                c.setId(50L);
                return c;
            });

            GrantRequest req = new GrantRequest(null, ConsentPurpose.HEALTH_INTEROPERABILITY,
                    "ICF_SCORE,RISK_LEVEL", "Ministerio de Salud y Protección Social", null);

            ConsentResponse result = consentService.grant(1L, req, "ana@test.com");

            assertThat(result.id()).isEqualTo(50L);
            assertThat(result.status()).isEqualTo(ConsentStatus.GRANTED);
            assertThat(result.active()).isTrue();
            assertThat(result.grantedByEmail()).isEqualTo("ana@test.com");

            ArgumentCaptor<Consent> captor = ArgumentCaptor.forClass(Consent.class);
            verify(consentRepository).save(captor.capture());
            assertThat(captor.getValue().getGranteeReference()).isEqualTo("Ministerio de Salud y Protección Social");

            verify(auditService).registerSystemEvent(eq("ana@test.com"), eq(AuditEventType.CONSENT_GRANTED), anyString());
        }

        @Test
        @DisplayName("consentimiento expirado → active() es false")
        void shouldReportInactive_whenExpired() {
            when(familyRepository.existsById(1L)).thenReturn(true);
            when(consentRepository.save(any(Consent.class))).thenAnswer(i -> i.getArgument(0));

            GrantRequest req = new GrantRequest(null, ConsentPurpose.ECOSYSTEM_SHARING, "PLAN_SUMMARY",
                    "Colegio ABC", LocalDateTime.now().minusDays(1));

            ConsentResponse result = consentService.grant(1L, req, "ana@test.com");

            assertThat(result.active()).isFalse();
        }
    }

    @Nested
    @DisplayName("revoke()")
    class Revoke {

        @Test
        @DisplayName("consentimiento no encontrado → BusinessException NOT_FOUND")
        void shouldThrow_whenNotFound() {
            when(consentRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> consentService.revoke(1L, 99L, new RevokeRequest("ya no aplica"), "ana@test.com"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("consentimiento de otra familia → BusinessException FORBIDDEN")
        void shouldThrow_whenWrongFamily() {
            Consent consent = Consent.builder().id(5L).familyId(2L).status(ConsentStatus.GRANTED).build();
            when(consentRepository.findById(5L)).thenReturn(Optional.of(consent));

            assertThatThrownBy(() -> consentService.revoke(1L, 5L, new RevokeRequest("x"), "ana@test.com"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getStatus())
                            .isEqualTo(org.springframework.http.HttpStatus.FORBIDDEN));
        }

        @Test
        @DisplayName("ya revocado → BusinessException")
        void shouldThrow_whenAlreadyRevoked() {
            Consent consent = Consent.builder().id(5L).familyId(1L).status(ConsentStatus.REVOKED).build();
            when(consentRepository.findById(5L)).thenReturn(Optional.of(consent));

            assertThatThrownBy(() -> consentService.revoke(1L, 5L, new RevokeRequest("x"), "ana@test.com"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("ya fue revocado");
        }

        @Test
        @DisplayName("caso exitoso → marca REVOKED, guarda razón y registra auditoría")
        void shouldRevoke_andAudit() {
            Consent consent = Consent.builder().id(5L).familyId(1L).status(ConsentStatus.GRANTED)
                    .grantedByEmail("ana@test.com").grantedAt(LocalDateTime.now()).build();
            when(consentRepository.findById(5L)).thenReturn(Optional.of(consent));
            when(consentRepository.save(any(Consent.class))).thenAnswer(i -> i.getArgument(0));

            ConsentResponse result = consentService.revoke(1L, 5L, new RevokeRequest("Ya no es necesario"), "ana@test.com");

            assertThat(result.status()).isEqualTo(ConsentStatus.REVOKED);
            assertThat(result.revokedByEmail()).isEqualTo("ana@test.com");
            assertThat(result.revocationReason()).isEqualTo("Ya no es necesario");
            assertThat(result.active()).isFalse();

            verify(auditService).registerSystemEvent(eq("ana@test.com"), eq(AuditEventType.CONSENT_REVOKED), anyString());
        }

        @Test
        @DisplayName("sin body de razón (null) → no lanza, revocationReason queda null")
        void shouldAllowNullReason() {
            Consent consent = Consent.builder().id(5L).familyId(1L).status(ConsentStatus.GRANTED).build();
            when(consentRepository.findById(5L)).thenReturn(Optional.of(consent));
            when(consentRepository.save(any(Consent.class))).thenAnswer(i -> i.getArgument(0));

            ConsentResponse result = consentService.revoke(1L, 5L, null, "ana@test.com");

            assertThat(result.revocationReason()).isNull();
        }
    }

    @Nested
    @DisplayName("list() / listActive()")
    class Queries {

        @Test
        @DisplayName("list() delega en el repositorio ordenado por fecha")
        void shouldList() {
            Consent c = Consent.builder().id(1L).familyId(1L).status(ConsentStatus.GRANTED).build();
            when(consentRepository.findByFamilyIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(c));

            assertThat(consentService.list(1L)).hasSize(1);
        }

        @Test
        @DisplayName("listActive() filtra los expirados aunque el estado siga GRANTED")
        void shouldFilterExpired() {
            Consent active = Consent.builder().id(1L).familyId(1L).status(ConsentStatus.GRANTED)
                    .expiresAt(LocalDateTime.now().plusDays(1)).build();
            Consent expired = Consent.builder().id(2L).familyId(1L).status(ConsentStatus.GRANTED)
                    .expiresAt(LocalDateTime.now().minusDays(1)).build();
            when(consentRepository.findByFamilyIdAndStatus(1L, ConsentStatus.GRANTED))
                    .thenReturn(List.of(active, expired));

            List<ConsentResponse> result = consentService.listActive(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(1L);
        }
    }
}
