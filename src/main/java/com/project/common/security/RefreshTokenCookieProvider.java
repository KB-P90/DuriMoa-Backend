package com.project.common.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RefreshTokenCookieProvider {

    public static final String COOKIE_NAME = "refreshToken";
    private static final String COOKIE_PATH = "/api/auth";
    private static final String SAME_SITE_POLICY = "None";

    private final Boolean secure;
    private final Duration expiration;

    public RefreshTokenCookieProvider(
            @Value("${auth.refresh-token-cookie-secure:false}") Boolean secure,
            @Value("${jwt.refresh-token-expiration:1209600000}") Long expirationMillis) {
        this.secure = secure;
        this.expiration = Duration.ofMillis(expirationMillis);
    }

    public ResponseCookie createCookie(String refreshToken) {
        return ResponseCookie.from(COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(secure)
                .sameSite(SAME_SITE_POLICY)
                .path(COOKIE_PATH)
                .maxAge(expiration)
                .build();
    }

    public ResponseCookie deleteCookie() {
        return ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite(SAME_SITE_POLICY)
                .path(COOKIE_PATH)
                .maxAge(Duration.ZERO)
                .build();
    }
}
