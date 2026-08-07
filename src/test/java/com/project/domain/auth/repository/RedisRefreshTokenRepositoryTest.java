package com.project.domain.auth.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisRefreshTokenRepositoryTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void setUp() {
        refreshTokenRepository = new RedisRefreshTokenRepository(redisTemplate);
    }

    @Test
    void saveStoresHashedTokenWithExpiration() {
        Duration expiration = Duration.ofDays(14);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        refreshTokenRepository.save(1L, "refreshToken", expiration);

        verify(valueOperations).set(
                eq("auth:refresh:1"),
                eq("e1eae9e373ba62a80cdbd4422fc002553447aeb38fdb8dbf511a52d3e8c5e417"),
                eq(expiration));
    }

    @Test
    void matchesReturnsTrueWhenTokenHashMatches() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("auth:refresh:1"))
                .thenReturn("e1eae9e373ba62a80cdbd4422fc002553447aeb38fdb8dbf511a52d3e8c5e417");

        assertTrue(refreshTokenRepository.matches(1L, "refreshToken"));
    }

    @Test
    void matchesReturnsFalseWhenTokenIsMissingOrDifferent() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("auth:refresh:1"))
                .thenReturn(null)
                .thenReturn("differentHash");

        assertFalse(refreshTokenRepository.matches(1L, "refreshToken"));
        assertFalse(refreshTokenRepository.matches(1L, "refreshToken"));
    }

    @Test
    void deleteRemovesUserRefreshToken() {
        refreshTokenRepository.delete(1L);

        verify(redisTemplate).delete("auth:refresh:1");
    }

    @Test
    void rotateAtomicallyReplacesMatchingToken() {
        Duration expiration = Duration.ofDays(14);
        when(redisTemplate.execute(
                org.mockito.ArgumentMatchers.<RedisScript<Long>>any(),
                eq(Collections.singletonList("auth:refresh:1")),
                anyString(),
                anyString(),
                eq("1209600000")))
                .thenReturn(1L);

        Boolean rotated = refreshTokenRepository.rotate(
                1L,
                "currentRefreshToken",
                "newRefreshToken",
                expiration);

        assertTrue(rotated);
    }

    @Test
    void rotateReturnsFalseWhenCurrentTokenDoesNotMatch() {
        Duration expiration = Duration.ofDays(14);
        when(redisTemplate.execute(
                org.mockito.ArgumentMatchers.<RedisScript<Long>>any(),
                eq(Collections.singletonList("auth:refresh:1")),
                anyString(),
                anyString(),
                eq("1209600000")))
                .thenReturn(0L);

        assertFalse(refreshTokenRepository.rotate(
                1L,
                "reusedRefreshToken",
                "newRefreshToken",
                expiration));
    }

    @Test
    void deleteIfMatchesAtomicallyDeletesMatchingToken() {
        when(redisTemplate.execute(
                org.mockito.ArgumentMatchers.<RedisScript<Long>>any(),
                eq(Collections.singletonList("auth:refresh:1")),
                anyString()))
                .thenReturn(1L);

        assertTrue(refreshTokenRepository.deleteIfMatches(1L, "refreshToken"));
    }

    @Test
    void deleteIfMatchesKeepsDifferentToken() {
        when(redisTemplate.execute(
                org.mockito.ArgumentMatchers.<RedisScript<Long>>any(),
                eq(Collections.singletonList("auth:refresh:1")),
                anyString()))
                .thenReturn(0L);

        assertFalse(refreshTokenRepository.deleteIfMatches(1L, "oldRefreshToken"));
    }
}
