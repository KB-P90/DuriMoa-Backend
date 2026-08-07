package com.project.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RefreshTokenCookieProviderTest {

    @Test
    void createsHttpOnlyRefreshTokenCookieForLocalEnvironment() {
        RefreshTokenCookieProvider cookieProvider =
                new RefreshTokenCookieProvider(false, 1209600000L);

        ResponseCookie cookie = cookieProvider.createCookie("refreshToken");

        assertEquals("refreshToken", cookie.getName());
        assertEquals("refreshToken", cookie.getValue());
        assertEquals("/api/auth", cookie.getPath());
        assertEquals(1209600L, cookie.getMaxAge().getSeconds());
        assertEquals("Lax", cookie.getSameSite());
        assertTrue(cookie.isHttpOnly());
        assertFalse(cookie.isSecure());
    }

    @Test
    void createsExpiredCookieForLogout() {
        RefreshTokenCookieProvider cookieProvider =
                new RefreshTokenCookieProvider(false, 1209600000L);

        ResponseCookie cookie = cookieProvider.deleteCookie();

        assertEquals("refreshToken", cookie.getName());
        assertEquals("", cookie.getValue());
        assertEquals("/api/auth", cookie.getPath());
        assertEquals(0L, cookie.getMaxAge().getSeconds());
        assertEquals("Lax", cookie.getSameSite());
        assertTrue(cookie.isHttpOnly());
    }
}
