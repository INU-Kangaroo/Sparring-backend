package com.kangaroo.sparring.global.security.oauth2.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RedisTemplate<String, String> redisTemplate;
    
    @Value("${jwt.refresh-token-validity}")
    private long refreshTokenValidityMs;
    
    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";

    /**
     * 리프레시 토큰을 Redis에 저장
     */
    public void saveRefreshToken(Long userId, String refreshToken) {
        String key = REFRESH_TOKEN_PREFIX + userId;
        Duration expiration = Duration.ofMillis(refreshTokenValidityMs);
        redisTemplate.opsForValue().set(key, refreshToken, expiration);
        log.info("Refresh token saved for user: {} with TTL: {} days", userId, expiration.toDays());
    }

    /**
     * Redis에서 리프레시 토큰 조회
     */
    public String getRefreshToken(Long userId) {
        String key = REFRESH_TOKEN_PREFIX + userId;
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * Redis에서 리프레시 토큰 삭제
     */
    public void deleteRefreshToken(Long userId) {
        String key = REFRESH_TOKEN_PREFIX + userId;
        redisTemplate.delete(key);
        log.info("Refresh token deleted for user: {}", userId);
    }

    /**
     * 리프레시 토큰 검증
     */
    public boolean validateRefreshToken(Long userId, String refreshToken) {
        String storedToken = getRefreshToken(userId);
        return storedToken != null && storedToken.equals(refreshToken);
    }
}
