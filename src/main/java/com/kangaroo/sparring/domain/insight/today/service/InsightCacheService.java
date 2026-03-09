package com.kangaroo.sparring.domain.insight.today.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class InsightCacheService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String KEY_PREFIX = "insight:";

    public Optional<String> get(String cacheKey) {
        try {
            String value = redisTemplate.opsForValue().get(cacheKey);
            return Optional.ofNullable(value);
        } catch (Exception e) {
            log.warn("인사이트 캐시 조회 실패: key={}", cacheKey, e);
            return Optional.empty();
        }
    }

    public void set(String cacheKey, String message, long ttlSeconds) {
        try {
            redisTemplate.opsForValue().set(cacheKey, message, Duration.ofSeconds(ttlSeconds));
        } catch (Exception e) {
            log.warn("인사이트 캐시 저장 실패: key={}", cacheKey, e);
        }
    }

    public String buildKey(Long userId, String date, String slotName) {
        return KEY_PREFIX + userId + ":" + date + ":" + slotName;
    }
}