package com.integrityfamily.familyhome.adapter;

import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.familyhome.port.FamilyProfileQueryPort;
import com.integrityfamily.familyhome.port.FamilyProfileSnapshot;
import com.integrityfamily.familyhome.security.FamilyIdentifierBridge;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** Family no tiene foto/imagen destacada persistida hoy; se expone null hasta que exista el campo. */
@Component
public class FamilyProfileQueryPortAdapter implements FamilyProfileQueryPort {

    private final FamilyRepository familyRepository;
    private final FamilyIdentifierBridge idBridge;

    public FamilyProfileQueryPortAdapter(FamilyRepository familyRepository, FamilyIdentifierBridge idBridge) {
        this.familyRepository = familyRepository;
        this.idBridge = idBridge;
    }

    @Override
    public FamilyProfileSnapshot getProfile(UUID familyId) {
        Optional<Long> fId = idBridge.resolveFamilyId(familyId);
        if (fId.isEmpty()) {
            return null;
        }
        Family family = familyRepository.findById(fId.get()).orElse(null);
        if (family == null) {
            return null;
        }
        return new FamilyProfileSnapshot(familyId, family.getName(), null, null);
    }
}
