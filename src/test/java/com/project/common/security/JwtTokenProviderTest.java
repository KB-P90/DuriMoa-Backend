package com.project.common.security;

import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenProviderTest {

    private static final String SECRET =
            Encoders.BASE64.encode(Keys.secretKeyFor(
                    io.jsonwebtoken.SignatureAlgorithm.HS256).getEncoded());

    @Test
    void createsAndParsesAccessToken() {
        JwtTokenProvider tokenProvider = new JwtTokenProvider(SECRET, 3600000L);

        String token = tokenProvider.createAccessToken(1L, "G");

        assertTrue(tokenProvider.validateToken(token));
        assertEquals(1L, tokenProvider.getUserId(token));
        assertEquals("G", tokenProvider.getRole(token));
    }

    @Test
    void rejectsExpiredToken() throws InterruptedException {
        JwtTokenProvider tokenProvider = new JwtTokenProvider(SECRET, 1L);
        String token = tokenProvider.createAccessToken(1L, "G");
        Thread.sleep(5L);

        assertFalse(tokenProvider.validateToken(token));
    }

    @Test
    void rejectsMalformedToken() {
        JwtTokenProvider tokenProvider = new JwtTokenProvider(SECRET, 3600000L);

        assertFalse(tokenProvider.validateToken("invalid-token"));
    }
}
