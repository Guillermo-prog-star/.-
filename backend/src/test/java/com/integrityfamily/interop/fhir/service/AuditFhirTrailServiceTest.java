package com.integrityfamily.interop.fhir.service;

import com.integrityfamily.common.exception.BusinessException;
import com.integrityfamily.domain.AuditEventType;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.FamilyMember;
import com.integrityfamily.domain.User;
import com.integrityfamily.domain.repository.AuditEventRepository;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.repository.MemberRepository;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditFhirTrailService — Unit Tests")
class AuditFhirTrailServiceTest {

    @Mock FamilyRepository familyRepository;
    @Mock MemberRepository memberRepository;
    @Mock AuditEventRepository auditEventRepository;

    @InjectMocks
    AuditFhirTrailService service;

    private Family family;

    @BeforeEach
    void setUp() {
        User owner = User.builder().id(1L).email("dueno@test.com").build();
        family = Family.builder().id(1L).name("Familia López").createdBy(owner).build();
    }

    @Test
    @DisplayName("familia inexistente → BusinessException NOT_FOUND")
    void shouldThrow_whenFamilyNotFound() {
        when(familyRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getFamilyAuditTrail(99L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("resuelve emails del dueño + miembros + email sintético de familia")
    void shouldResolveAllRelevantEmails() {
        when(familyRepository.findById(1L)).thenReturn(Optional.of(family));

        FamilyMember member = new FamilyMember();
        member.setId(10L); member.setEmail("hijo@test.com");
        when(memberRepository.findByFamilyId(1L)).thenReturn(List.of(member));

        when(auditEventRepository.findByActorEmailInOrderByOccurredAtDesc(anyList())).thenReturn(List.of());

        service.getFamilyAuditTrail(1L);

        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        org.mockito.Mockito.verify(auditEventRepository).findByActorEmailInOrderByOccurredAtDesc(captor.capture());

        assertThat(captor.getValue()).containsExactlyInAnyOrder(
                "dueno@test.com", "hijo@test.com", "family_1@integrityfamily.com");
    }

    @Test
    @DisplayName("miembro sin email → se filtra, no genera null en la lista")
    void shouldFilterOutNullMemberEmails() {
        when(familyRepository.findById(1L)).thenReturn(Optional.of(family));

        FamilyMember memberNoEmail = new FamilyMember();
        memberNoEmail.setId(11L); memberNoEmail.setEmail(null);
        when(memberRepository.findByFamilyId(1L)).thenReturn(List.of(memberNoEmail));
        when(auditEventRepository.findByActorEmailInOrderByOccurredAtDesc(anyList())).thenReturn(List.of());

        service.getFamilyAuditTrail(1L);

        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        org.mockito.Mockito.verify(auditEventRepository).findByActorEmailInOrderByOccurredAtDesc(captor.capture());
        assertThat(captor.getValue()).doesNotContainNull();
    }

    @Test
    @DisplayName("assembleBundle() envuelve los AuditEvent en un Bundle COLLECTION")
    void shouldAssembleBundle() {
        when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
        when(memberRepository.findByFamilyId(1L)).thenReturn(List.of());

        com.integrityfamily.domain.AuditEvent event = com.integrityfamily.domain.AuditEvent.builder()
                .id(1L).eventType(AuditEventType.LOGIN_SUCCESS).occurredAt(LocalDateTime.now()).build();
        when(auditEventRepository.findByActorEmailInOrderByOccurredAtDesc(anyList())).thenReturn(List.of(event));

        Bundle bundle = service.assembleBundle(1L);

        assertThat(bundle.getType()).isEqualTo(Bundle.BundleType.COLLECTION);
        assertThat(bundle.getId()).isEqualTo("family-1-audit-trail");
        assertThat(bundle.getEntry()).hasSize(1);
    }
}
