package com.integrityfamily.security;

import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
public class TenantInterceptor implements HandlerInterceptor {

    private final EntityManager entityManager;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Long familyId = TenantContext.getCurrentFamilyId();
        if (familyId != null) {
            Session session = entityManager.unwrap(Session.class);
            session.enableFilter("familyFilter").setParameter("familyId", familyId);
            log.debug("[TENANT] Filtro de familia activado en Interceptor para ID: {}", familyId);
        }
        return true;
    }
}
