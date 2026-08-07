package com.project.domain.auth.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Collections;
import java.util.HexFormat;

@Repository
@RequiredArgsConstructor
public class RedisRefreshTokenRepository implements RefreshTokenRepository {

    private static final String KEY_PREFIX = "auth:refresh:";
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final DefaultRedisScript<Long> ROTATE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('GET', KEYS[1]) == ARGV[1] then "
                    + "redis.call('PSETEX', KEYS[1], ARGV[3], ARGV[2]); return 1 "
                    + "else return 0 end",
            Long.class);
    private static final DefaultRedisScript<Long> DELETE_IF_MATCHES_SCRIPT =
            new DefaultRedisScript<>(
                    "if redis.call('GET', KEYS[1]) == ARGV[1] then "
                            + "return redis.call('DEL', KEYS[1]) else return 0 end",
                    Long.class);

    private final StringRedisTemplate redisTemplate;

    @Override
    public void save(Long userId, String refreshToken, Duration expiration) {
        redisTemplate.opsForValue().set(createKey(userId), hash(refreshToken), expiration);
    }

    @Override
    public Boolean matches(Long userId, String refreshToken) {
        String storedTokenHash = redisTemplate.opsForValue().get(createKey(userId));
        if (storedTokenHash == null) {
            return false;
        }

        byte[] storedHashBytes = storedTokenHash.getBytes(StandardCharsets.UTF_8);
        byte[] requestedHashBytes = hash(refreshToken).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(storedHashBytes, requestedHashBytes);
    }

    @Override
    public Boolean rotate(
            Long userId,
            String currentRefreshToken,
            String newRefreshToken,
            Duration expiration) {
        Long result = redisTemplate.execute(
                ROTATE_SCRIPT,
                Collections.singletonList(createKey(userId)),
                hash(currentRefreshToken),
                hash(newRefreshToken),
                String.valueOf(expiration.toMillis()));
        return Long.valueOf(1L).equals(result);
    }

    @Override
    public Boolean deleteIfMatches(Long userId, String refreshToken) {
        Long result = redisTemplate.execute(
                DELETE_IF_MATCHES_SCRIPT,
                Collections.singletonList(createKey(userId)),
                hash(refreshToken));
        return Long.valueOf(1L).equals(result);
    }

    @Override
    public void delete(Long userId) {
        redisTemplate.delete(createKey(userId));
    }

    private String createKey(Long userId) {
        return KEY_PREFIX + userId;
    }

    private String hash(String refreshToken) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] digest = messageDigest.digest(refreshToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Refresh Token 해시 알고리즘을 사용할 수 없습니다.", exception);
        }
    }
}
