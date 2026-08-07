package com.project.common.security;

import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenProviderTest {

    private static final Long ACCESS_TOKEN_EXPIRATION = 3600000L;
    private static final Long REFRESH_TOKEN_EXPIRATION = 1209600000L;
    private static final String SECRET =
            Encoders.BASE64.encode(Keys.secretKeyFor(
                    io.jsonwebtoken.SignatureAlgorithm.HS256).getEncoded());

    @Test
    void createsAndParsesAccessToken() {
        JwtTokenProvider tokenProvider = createTokenProvider(ACCESS_TOKEN_EXPIRATION);

        String token = tokenProvider.createAccessToken(1L, "G");

        assertTrue(tokenProvider.validateToken(token));
        assertTrue(tokenProvider.validateAccessToken(token));
        assertFalse(tokenProvider.validateRefreshToken(token));
        assertEquals(1L, tokenProvider.getUserId(token));
        assertEquals("G", tokenProvider.getRole(token));
    }

    @Test
    void createsAndParsesRefreshToken() {
        JwtTokenProvider tokenProvider = createTokenProvider(ACCESS_TOKEN_EXPIRATION);

        String token = tokenProvider.createRefreshToken(1L);

        assertTrue(tokenProvider.validateToken(token));
        assertTrue(tokenProvider.validateRefreshToken(token));
        assertFalse(tokenProvider.validateAccessToken(token));
        assertEquals(1L, tokenProvider.getUserId(token));
        assertEquals(Duration.ofDays(14), tokenProvider.getRefreshTokenExpiration());
    }

    @Test
    void rejectsExpiredToken() throws InterruptedException {
        JwtTokenProvider tokenProvider = createTokenProvider(1L);
        String token = tokenProvider.createAccessToken(1L, "G");
        Thread.sleep(5L);

        assertFalse(tokenProvider.validateToken(token));
        assertFalse(tokenProvider.validateAccessToken(token));
    }

    @Test
    void rejectsMalformedToken() {
        JwtTokenProvider tokenProvider = createTokenProvider(ACCESS_TOKEN_EXPIRATION);

        assertFalse(tokenProvider.validateToken("invalid-token"));
        assertFalse(tokenProvider.validateAccessToken("invalid-token"));
        assertFalse(tokenProvider.validateRefreshToken("invalid-token"));
    }

    private JwtTokenProvider createTokenProvider(Long accessTokenExpiration) {
        return new JwtTokenProvider(
                SECRET,
                accessTokenExpiration,
                REFRESH_TOKEN_EXPIRATION);
    }
}
