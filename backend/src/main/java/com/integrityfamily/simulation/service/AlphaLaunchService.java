package com.integrityfamily.simulation.service;

import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.User;
import com.integrityfamily.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AlphaLaunchService {

    private final FamilyRepository familyRepository;
    private final UserRepository userRepository;

    @Transactional
    public List<Family> provisionAlphaFamilies(int count, String creatorEmail) {
        log.info("Ã°Å¸Å¡â‚¬ [ALPHA-LAUNCH] Provisionando {} nÃƒÂºcleos familiares para la Fase Alfa...", count);
        
        User creator = userRepository.findByEmail(creatorEmail)
                .orElseThrow(() -> new RuntimeException("Usuario creador no encontrado: " + creatorEmail));

        List<Family> alphaFamilies = new ArrayList<>();
        
        for (int i = 1; i <= count; i++) {
            String alphaId = String.format("%03d", i);
            String code = "ALFA-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            
            Family family = Family.builder()
                    .name("NÃƒÂºcleo Alfa " + alphaId)
                    .description("Nodo oficial de la Fase Alfa de Integrity Family. ValidaciÃƒÂ³n pedagÃƒÂ³gica nivel 1.")
                    .familyCode(code)
                    .pin("1234") // PIN genÃƒÂ©rico para facilitar el onboarding inicial
                    .currentMilestone("MES_00_DIAGNOSTICO")
                    .createdBy(creator)
                    .build();
            
            alphaFamilies.add(familyRepository.save(family));
        }

        log.info("Ã¢Å“â€¦ Despliegue completado. {} familias listas para ser habitadas.", count);
        return alphaFamilies;
    }
}


