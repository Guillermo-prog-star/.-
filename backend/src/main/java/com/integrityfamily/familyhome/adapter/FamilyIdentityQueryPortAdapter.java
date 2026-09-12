package com.integrityfamily.familyhome.adapter;

import com.integrityfamily.familyhome.port.FamilyIdentityQueryPort;
import com.integrityfamily.familyhome.security.FamilyIdentifierBridge;
import com.integrityfamily.legado.domain.FamilyLegacy;
import com.integrityfamily.legado.repository.FamilyLegacyRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class FamilyIdentityQueryPortAdapter implements FamilyIdentityQueryPort {

    private final FamilyLegacyRepository familyLegacyRepository;
    private final FamilyIdentifierBridge idBridge;

    public FamilyIdentityQueryPortAdapter(FamilyLegacyRepository familyLegacyRepository, FamilyIdentifierBridge idBridge) {
        this.familyLegacyRepository = familyLegacyRepository;
        this.idBridge = idBridge;
    }

    @Override
    public String getLema(UUID familyId) {
        Optional<Long> fId = idBridge.resolveFamilyId(familyId);
        if (fId.isEmpty()) {
            return null;
        }
        return familyLegacyRepository.findByFamilyId(fId.get())
                .map(FamilyLegacy::getFamilyTagline)
                .orElse(null);
    }
}
