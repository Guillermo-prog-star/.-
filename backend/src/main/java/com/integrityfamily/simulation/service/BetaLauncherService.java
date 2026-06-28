package com.integrityfamily.simulation.service;

import com.integrityfamily.member.service.MemberService;
import com.integrityfamily.member.dto.MemberRequest;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.repository.FamilyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BetaLauncherService {

    private final MemberService memberService;
    private final FamilyRepository familyRepository;

    @Transactional
    public String launch(String email) {
        Family family = familyRepository.findByCreatedBy_Email(email)
                .orElseThrow(() -> new RuntimeException("No se encontr? familia asociada al email: " + email));
        launchSimulation(family.getId());
        return "Beta launch completado para la familia: " + family.getName()
                + " (c?digo: " + family.getFamilyCode() + ")";
    }

    @Transactional
    public void launchSimulation(Long familyId) {
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new RuntimeException("Familia no encontrada"));

        log.info(">>>> [SIMULACIÓN] Iniciando Nodo Alfa para: {}", family.getName());

        // SDD: Sincronización Estricta con el Record (6 parámetros exactos)
        MemberRequest leader = new MemberRequest(
                "L?der de Prueba",    // fullName
                "PADRE",              // roleType
                40,                   // age
                5,                    // autonomyLevel
                5,                    // responsibilityLevel
                null,                 // email
                null,                 // phone
                familyId              // familyId
        );

        // SDD: Llamada al servicio simplificada para evitar errores de firma
        memberService.createMember(leader); 

        family.setSentinelActive(true);
        familyRepository.save(family);

        log.info(">>>> [SIMULACIÓN] ÉXITO: Sentinel activado para {}", family.getFamilyCode());
    }
}


