package com.project.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private static final Logger log = LogManager.getLogger(JwtTokenProvider.class);
    private static final String ROLE_CLAIM = "role";

    private final Key signingKey;
    private final Long accessTokenExpiration;

    public JwtTokenProvider(
            @Value("${jwt.secret:}") String secret,
            @Value("${jwt.access-token-expiration:3600000}") Long accessTokenExpiration) {
        this.signingKey = createSigningKey(secret);
        this.accessTokenExpiration = accessTokenExpiration;
    }

    public String createAccessToken(Long userId, String role) {
        Date issuedAt = new Date();
        Date expiration = new Date(issuedAt.getTime() + accessTokenExpiration);

        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim(ROLE_CLAIM, role)
                .setIssuedAt(issuedAt)
                .setExpiration(expiration)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public Boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    public Long getUserId(String token) {
        return Long.valueOf(parseClaims(token).getSubject());
    }

    public String getRole(String token) {
        return parseClaims(token).get(ROLE_CLAIM, String.class);
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key createSigningKey(String secret) {
        if (secret == null || secret.isBlank()) {
            log.warn("JWT 비밀키가 설정되지 않아 서버 재시작 시 변경되는 임시 키를 사용합니다.");
            return Keys.secretKeyFor(SignatureAlgorithm.HS256);
        }

        try {
            SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
            return key;
        } catch (IllegalArgumentException exception) {
            byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
            return Keys.hmacShaKeyFor(keyBytes);
        }
    }
}
