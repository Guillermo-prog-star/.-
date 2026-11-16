package com.integrityfamily.familyhome.security;

import com.integrityfamily.domain.User;
import com.integrityfamily.domain.repository.UserRepository;
import com.integrityfamily.familyhome.api.AuthenticatedUserResolver;
import com.integrityfamily.familyhome.application.exception.FamilyHomeAuthenticationMappingException;
import com.integrityfamily.familyhome.application.exception.FamilyHomeUnauthenticatedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public final class SpringSecurityAuthenticatedUserResolver implements AuthenticatedUserResolver {

    private final UserRepository userRepository;
    private final FamilyIdentifierBridge idBridge;

    public SpringSecurityAuthenticatedUserResolver(UserRepository userRepository, FamilyIdentifierBridge idBridge) {
        this.userRepository = userRepository;
        this.idBridge = idBridge;
    }

    @Override
    public UUID requireAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new FamilyHomeUnauthenticatedException();
        }

        Object principal = authentication.getPrincipal();
        String email = null;

        if (principal instanceof UserDetails userDetails) {
            email = userDetails.getUsername();
        } else if (principal instanceof String principalString) {
            email = principalString;
        }

        if (email == null || email.trim().isEmpty()) {
            throw new FamilyHomeAuthenticationMappingException("No email found in security principal context");
        }

        final String finalEmail = email;
        User user = userRepository.findByEmailIgnoreCase(finalEmail)
                .orElseThrow(() -> new FamilyHomeAuthenticationMappingException("Authenticated user not found in database: " + finalEmail));

        return idBridge.toUserUuid(user.getId());
    }
}
