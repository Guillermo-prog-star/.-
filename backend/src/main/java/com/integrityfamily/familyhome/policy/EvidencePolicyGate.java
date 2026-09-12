package com.integrityfamily.familyhome.policy;

import com.integrityfamily.familyhome.port.NarrativeCandidate;

public class EvidencePolicyGate {
    public static boolean isAllowed(NarrativeCandidate candidate) {
        if (candidate == null) {
            return false;
        }
        String policy = candidate.rawEvidencePolicy();
        // Strict deny-by-default logic: only FAMILY_APPROVED narratives are projected to the family
        return "FAMILY_APPROVED".equals(policy);
    }
}
