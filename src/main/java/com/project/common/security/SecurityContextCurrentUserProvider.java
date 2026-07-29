package com.project.common.security;

import com.project.common.exception.AuthenticationRequiredException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityContextCurrentUserProvider implements CurrentUserProvider {

    @Override
    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AuthenticationRequiredException();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Number) {
            return ((Number) principal).longValue();
        }

        if (principal instanceof String) {
            try {
                return Long.valueOf((String) principal);
            } catch (NumberFormatException exception) {
                throw new AuthenticationRequiredException();
            }
        }

        throw new AuthenticationRequiredException();
    }
}
