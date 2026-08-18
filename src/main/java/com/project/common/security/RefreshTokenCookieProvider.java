package com.project.common.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RefreshTokenCookieProvider {

    public static final String COOKIE_NAME = "refreshToken";
    private static final String COOKIE_PATH = "/api/auth";
    private static final String SAME_SITE_POLICY = "Lax";
    private static final boolean COOKIE_SECURE = true;

    private final Duration expiration;

    public RefreshTokenCookieProvider(
            @Value("${jwt.refresh-token-expiration:1209600000}") Long expirationMillis) {
        this.expiration = Duration.ofMillis(expirationMillis);
    }

    public ResponseCookie createCookie(String refreshToken) {
        return ResponseCookie.from(COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(COOKIE_SECURE)
                .sameSite(SAME_SITE_POLICY)
                .path(COOKIE_PATH)
                .maxAge(expiration)
                .build();
    }

    public ResponseCookie deleteCookie() {
        return ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(COOKIE_SECURE)
                .sameSite(SAME_SITE_POLICY)
                .path(COOKIE_PATH)
                .maxAge(Duration.ZERO)
                .build();
    }
}
