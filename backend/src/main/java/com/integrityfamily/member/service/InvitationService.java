package com.integrityfamily.member.service;

import com.integrityfamily.auth.service.EmailService;
import com.integrityfamily.common.exception.BusinessException;
import com.integrityfamily.common.service.WhatsAppService;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.FamilyMember;
import com.integrityfamily.domain.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvitationService {

    private final MemberRepository memberRepository;
    private final WhatsAppService whatsAppService;
    private final EmailService emailService;

    public void sendInvitation(Long memberId) {
        FamilyMember member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException("Miembro no encontrado: " + memberId, "MEMBER_NOT_FOUND", HttpStatus.NOT_FOUND));

        Family family = member.getFamily();
        String familyName = family.getName();
        String familyCode = family.getFamilyCode();
        
        // Si no hay código de familia, usamos uno genérico o lo generamos (SDD: Debería existir post-registro)
        if (familyCode == null || familyCode.isEmpty()) {
            familyCode = "FAM-" + family.getId();
        }

        log.info("[INVITATION] Procesando invitaciones para: {} de la familia {}", member.getFullName(), familyName);

        // Envío por WhatsApp si tiene teléfono
        if (member.getPhone() != null && !member.getPhone().isEmpty()) {
            whatsAppService.sendInvitation(member.getPhone(), member.getFirstName(), familyName, familyCode);
        } else if (family.getWhatsapp() != null) {
            // Fallback al WhatsApp de la familia si el miembro no tiene uno propio (contexto familiar)
            whatsAppService.sendInvitation(family.getWhatsapp(), member.getFirstName(), familyName, familyCode);
        }

        // Envío por Email si tiene
        if (member.getEmail() != null && !member.getEmail().isEmpty()) {
            emailService.sendInvitation(member.getEmail(), member.getFirstName(), familyName, familyCode);
        }
    }
}
