package com.integrityfamily.familyhome.port;

import java.util.UUID;

public interface FamilyMemoryQueryPort {
    MemoryCandidate getFeaturedMemory(UUID familyId);
}
