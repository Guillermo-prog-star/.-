package com.integrityfamily.analytics.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AnalyticsService {

    public Map<String, Object> getDashboardSummary() {
        Map<String, Object> data = new HashMap<>();
        data.put("status", "ok");
        return data;
    }

    public Map<String, Object> getFamilySummary(Long familyId) {
        Map<String, Object> data = new HashMap<>();
        data.put("familyId", familyId);
        data.put("status", "ok");
        return data;
    }
}