package com.integrityfamily.security;

public class TenantContext {
    private static final ThreadLocal<Long> currentFamilyId = new ThreadLocal<>();

    public static void setCurrentFamilyId(Long familyId) {
        currentFamilyId.set(familyId);
    }

    public static Long getCurrentFamilyId() {
        return currentFamilyId.get();
    }

    public static void clear() {
        currentFamilyId.remove();
    }
}
