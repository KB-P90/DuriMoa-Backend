package com.project.domain.auth.repository;

import java.time.Duration;

public interface RefreshTokenRepository {

    void save(Long userId, String refreshToken, Duration expiration);

    Boolean matches(Long userId, String refreshToken);

    Boolean rotate(
            Long userId,
            String currentRefreshToken,
            String newRefreshToken,
            Duration expiration);

    Boolean deleteIfMatches(Long userId, String refreshToken);

    void delete(Long userId);
}
