package com.kangaroo.sparring.domain.chatbot.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kangaroo.sparring.domain.chatbot.entity.ChatMessage;
import com.kangaroo.sparring.domain.chatbot.entity.ChatSession;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatSessionRepository {

    private static final String SESSION_KEY_PREFIX = "chatbot:session:";
    private static final String USER_SESSIONS_ZSET_KEY_PREFIX = "chatbot:sessions:zset:";
    private static final String SESSION_LOCK_KEY_PREFIX = "chatbot:session:lock:";
    private static final Duration SESSION_TTL = Duration.ofHours(24);
    private static final Duration LOCK_TTL = Duration.ofSeconds(3);
    private static final Duration LOCK_WAIT_TIMEOUT = Duration.ofSeconds(1);
    private static final long LOCK_RETRY_INTERVAL_MILLIS = 20L;
    private static final RedisScript<Long> RELEASE_LOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class
    );

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public void save(ChatSession session) {
        String sessionKey = sessionKey(session.getUserId(), session.getSessionId());
        String userZsetKey = userZsetKey(session.getUserId());
        String lockKey = lockKey(session.getUserId(), session.getSessionId());
        String lockToken = UUID.randomUUID().toString();

        if (!acquireLock(lockKey, lockToken)) {
            throw new CustomException(ErrorCode.CHATBOT_SESSION_SERIALIZE_FAILED, "채팅 세션 저장이 지연되고 있습니다. 잠시 후 다시 시도해주세요.");
        }

        try {
            ChatSession mergedSession = mergeWithExistingSession(sessionKey, session);
            String json = objectMapper.writeValueAsString(mergedSession);
            redisTemplate.opsForValue().set(sessionKey, json, SESSION_TTL);
            LocalDateTime lastActiveAt = mergedSession.getLastActiveAt() != null
                    ? mergedSession.getLastActiveAt()
                    : com.kangaroo.sparring.global.support.KstDateTimeSupport.nowDateTime();
            redisTemplate.opsForZSet().add(
                    userZsetKey,
                    mergedSession.getSessionId(),
                    lastActiveAt.atZone(com.kangaroo.sparring.global.support.KstDateTimeSupport.zoneId()).toInstant().toEpochMilli()
            );
            redisTemplate.expire(userZsetKey, SESSION_TTL);
        } catch (JsonProcessingException e) {
            log.error("채팅 세션 직렬화 실패: sessionId={}", session.getSessionId(), e);
            throw new CustomException(ErrorCode.CHATBOT_SESSION_SERIALIZE_FAILED);
        } finally {
            releaseLock(lockKey, lockToken);
        }
    }

    private ChatSession mergeWithExistingSession(String sessionKey, ChatSession incoming) throws JsonProcessingException {
        String existingJson = redisTemplate.opsForValue().get(sessionKey);
        if (existingJson == null || existingJson.isBlank()) {
            return incoming;
        }

        ChatSession existing = objectMapper.readValue(existingJson, ChatSession.class);
        List<ChatMessage> mergedMessages = new ArrayList<>();
        if (existing.getMessages() != null) {
            mergedMessages.addAll(existing.getMessages());
        }
        if (incoming.getMessages() != null) {
            for (ChatMessage incomingMessage : incoming.getMessages()) {
                if (!containsMessage(mergedMessages, incomingMessage)) {
                    mergedMessages.add(incomingMessage);
                }
            }
        }

        String mergedTitle = resolveTitle(existing.getTitle(), incoming.getTitle());
        LocalDateTime mergedCreatedAt = existing.getCreatedAt() != null ? existing.getCreatedAt() : incoming.getCreatedAt();
        LocalDateTime mergedLastActiveAt = maxTime(existing.getLastActiveAt(), incoming.getLastActiveAt());

        return ChatSession.builder()
                .sessionId(incoming.getSessionId())
                .userId(incoming.getUserId())
                .title(mergedTitle)
                .messages(mergedMessages)
                .createdAt(mergedCreatedAt)
                .lastActiveAt(mergedLastActiveAt)
                .build();
    }

    private boolean containsMessage(List<ChatMessage> messages, ChatMessage candidate) {
        return messages.stream().anyMatch(message ->
                message.getRole() == candidate.getRole()
                        && Objects.equals(message.getContent(), candidate.getContent())
                        && Objects.equals(message.getTimestamp(), candidate.getTimestamp())
        );
    }

    private String resolveTitle(String existingTitle, String incomingTitle) {
        if (incomingTitle == null || incomingTitle.isBlank()) {
            return existingTitle;
        }
        if (existingTitle == null || existingTitle.isBlank() || "새 대화".equals(existingTitle)) {
            return incomingTitle;
        }
        return existingTitle;
    }

    private LocalDateTime maxTime(LocalDateTime first, LocalDateTime second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        return first.isAfter(second) ? first : second;
    }

    private boolean acquireLock(String lockKey, String lockToken) {
        long deadlineNanos = System.nanoTime() + LOCK_WAIT_TIMEOUT.toNanos();
        while (System.nanoTime() < deadlineNanos) {
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, lockToken, LOCK_TTL);
            if (Boolean.TRUE.equals(acquired)) {
                return true;
            }
            try {
                Thread.sleep(LOCK_RETRY_INTERVAL_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    private void releaseLock(String lockKey, String lockToken) {
        try {
            redisTemplate.execute(RELEASE_LOCK_SCRIPT, List.of(lockKey), lockToken);
        } catch (Exception e) {
            log.warn("채팅 세션 락 해제 실패: key={}", lockKey, e);
        }
    }

    public Optional<ChatSession> findById(Long userId, String sessionId) {
        String key = sessionKey(userId, sessionId);
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, ChatSession.class));
        } catch (JsonProcessingException e) {
            log.error("채팅 세션 역직렬화 실패: sessionId={}", sessionId, e);
            throw new CustomException(ErrorCode.CHATBOT_SESSION_DESERIALIZE_FAILED);
        }
    }

    public List<ChatSession> findAllByUserId(Long userId) {
        return findRecentByUserId(userId, Integer.MAX_VALUE);
    }

    public List<ChatSession> findRecentByUserId(Long userId, int limit) {
        if (limit <= 0) {
            return List.of();
        }
        String userZsetKey = userZsetKey(userId);
        Set<String> sessionIds = redisTemplate.opsForZSet().reverseRange(userZsetKey, 0, limit - 1L);
        if (sessionIds == null || sessionIds.isEmpty()) {
            return List.of();
        }
        return sessionIds.stream()
                .map(sid -> findById(userId, sid))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    public void delete(Long userId, String sessionId) {
        redisTemplate.delete(sessionKey(userId, sessionId));
        redisTemplate.opsForZSet().remove(userZsetKey(userId), sessionId);
    }

    public boolean existsById(Long userId, String sessionId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(sessionKey(userId, sessionId)));
    }

    private String sessionKey(Long userId, String sessionId) {
        return SESSION_KEY_PREFIX + userId + ":" + sessionId;
    }

    private String userZsetKey(Long userId) {
        return USER_SESSIONS_ZSET_KEY_PREFIX + userId;
    }

    private String lockKey(Long userId, String sessionId) {
        return SESSION_LOCK_KEY_PREFIX + userId + ":" + sessionId;
    }
}
