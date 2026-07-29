package com.project.common.security;

import com.project.common.exception.AuthenticationRequiredException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SecurityContextCurrentUserProviderTest {

    private final SecurityContextCurrentUserProvider provider = new SecurityContextCurrentUserProvider();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void extractsUserIdFromNumericPrincipal() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(123L, null, Collections.emptyList())
        );

        assertEquals(123L, provider.getCurrentUserId());
    }

    @Test
    void rejectsUnauthenticatedUser() {
        assertThrows(AuthenticationRequiredException.class, provider::getCurrentUserId);
    }

    @Test
    void rejectsPrincipalWithoutNumericUserId() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("phone-number", null, Collections.emptyList())
        );

        assertThrows(AuthenticationRequiredException.class, provider::getCurrentUserId);
    }
}
