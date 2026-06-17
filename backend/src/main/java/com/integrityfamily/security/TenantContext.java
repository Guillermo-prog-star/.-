package com.integrityfamily.security;

public class TenantContext {
    private static final ThreadLocal<String> currentTenant = new ThreadLocal<>();
    private static final ThreadLocal<Long> currentFamilyId = new ThreadLocal<>();

    public static void setCurrentTenant(String tenantId) {
        currentTenant.set(tenantId);
    }

    public static String getCurrentTenant() {
        return currentTenant.get();
    }

    public static void setCurrentFamilyId(Long familyId) {
        currentFamilyId.set(familyId);
    }

    public static Long getCurrentFamilyId() {
        return currentFamilyId.get();
    }

    public static void clear() {
        currentTenant.remove();
        currentFamilyId.remove();
    }
}
