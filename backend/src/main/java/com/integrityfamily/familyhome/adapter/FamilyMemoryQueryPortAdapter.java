package com.integrityfamily.familyhome.adapter;

import com.integrityfamily.domain.FamilyDocumentary;
import com.integrityfamily.domain.repository.FamilyDocumentaryRepository;
import com.integrityfamily.familyhome.port.FamilyMemoryQueryPort;
import com.integrityfamily.familyhome.port.MemoryCandidate;
import com.integrityfamily.familyhome.security.FamilyIdentifierBridge;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

/** FamilyDocumentary no tiene columna de asset multimedia; assetUrl queda null hasta que exista. */
@Component
public class FamilyMemoryQueryPortAdapter implements FamilyMemoryQueryPort {

    private final FamilyDocumentaryRepository documentaryRepository;
    private final FamilyIdentifierBridge idBridge;

    public FamilyMemoryQueryPortAdapter(FamilyDocumentaryRepository documentaryRepository, FamilyIdentifierBridge idBridge) {
        this.documentaryRepository = documentaryRepository;
        this.idBridge = idBridge;
    }

    @Override
    public MemoryCandidate getFeaturedMemory(UUID familyId) {
        Optional<Long> fId = idBridge.resolveFamilyId(familyId);
        if (fId.isEmpty()) {
            return null;
        }

        FamilyDocumentary documentary = documentaryRepository.findByFamilyIdOrderByCreatedAtDesc(fId.get()).stream()
                .findFirst()
                .orElse(null);
        if (documentary == null) {
            return null;
        }

        return new MemoryCandidate(
                UUID.nameUUIDFromBytes(("documentary-" + documentary.getId()).getBytes(StandardCharsets.UTF_8)),
                documentary.getTitle(),
                documentary.getCreatedAt() != null
                        ? documentary.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant()
                        : null,
                null
        );
    }
}
