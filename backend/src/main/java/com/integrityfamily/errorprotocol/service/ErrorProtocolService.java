package com.integrityfamily.errorprotocol.service;

import com.integrityfamily.errorprotocol.domain.FamilyErrorProtocol;
import com.integrityfamily.errorprotocol.domain.FamilyErrorProtocol.ProtocolStep;
import com.integrityfamily.errorprotocol.repository.ErrorProtocolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ErrorProtocolService {

    private final ErrorProtocolRepository repo;

    public List<FamilyErrorProtocol> getAll(Long familyId) {
        return repo.findByFamilyIdOrderByCreatedAtDesc(familyId);
    }

    public List<FamilyErrorProtocol> getOpen(Long familyId) {
        return repo.findByFamilyIdAndClosedFalse(familyId);
    }

    @Transactional
    public FamilyErrorProtocol create(Long familyId, String missionFailed) {
        return repo.save(FamilyErrorProtocol.builder()
                .familyId(familyId)
                .missionFailed(missionFailed)
                .build());
    }

    @Transactional
    public FamilyErrorProtocol update(Long id, Map<String, Object> fields) {
        FamilyErrorProtocol p = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Protocolo no encontrado: " + id));

        if (fields.containsKey("missionFailed"))   p.setMissionFailed((String) fields.get("missionFailed"));
        if (fields.containsKey("feelings"))         p.setFeelings((String) fields.get("feelings"));
        if (fields.containsKey("whatHappened"))     p.setWhatHappened((String) fields.get("whatHappened"));
        if (fields.containsKey("correctiveAction")) p.setCorrectiveAction((String) fields.get("correctiveAction"));
        if (fields.containsKey("whoHelps"))         p.setWhoHelps((String) fields.get("whoHelps"));
        if (fields.containsKey("agreement"))        p.setAgreement((String) fields.get("agreement"));
        if (fields.containsKey("learning"))         p.setLearning((String) fields.get("learning"));
        if (fields.containsKey("currentStep")) {
            p.setCurrentStep(ProtocolStep.valueOf((String) fields.get("currentStep")));
        }

        return repo.save(p);
    }

    @Transactional
    public FamilyErrorProtocol close(Long id, String learning) {
        FamilyErrorProtocol p = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Protocolo no encontrado: " + id));
        p.setLearning(learning);
        p.setClosed(true);
        p.setClosedAt(LocalDateTime.now());
        p.setCurrentStep(ProtocolStep.LEARNING);
        return repo.save(p);
    }
}
