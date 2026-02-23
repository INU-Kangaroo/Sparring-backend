package com.kangaroo.sparring.domain.chatbot.session;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatSessionRepository {

    private static final String SESSION_KEY_PREFIX = "chatbot:session:";
    private static final String USER_SESSIONS_KEY_PREFIX = "chatbot:sessions:";
    private static final Duration SESSION_TTL = Duration.ofHours(24);

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public void save(ChatSession session) {
        String sessionKey = sessionKey(session.getUserId(), session.getSessionId());
        String userSetKey = userSetKey(session.getUserId());
        try {
            String json = objectMapper.writeValueAsString(session);
            redisTemplate.opsForValue().set(sessionKey, json, SESSION_TTL);
            redisTemplate.opsForSet().add(userSetKey, session.getSessionId());
            redisTemplate.expire(userSetKey, SESSION_TTL);
        } catch (JsonProcessingException e) {
            log.error("채팅 세션 직렬화 실패: sessionId={}", session.getSessionId(), e);
            throw new CustomException(ErrorCode.CHATBOT_SESSION_SERIALIZE_FAILED);
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
        String userSetKey = userSetKey(userId);
        Set<String> sessionIds = redisTemplate.opsForSet().members(userSetKey);
        if (sessionIds == null || sessionIds.isEmpty()) {
            return List.of();
        }
        return sessionIds.stream()
                .map(sid -> findById(userId, sid))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .sorted(Comparator.comparing(ChatSession::getLastActiveAt).reversed())
                .toList();
    }

    public void delete(Long userId, String sessionId) {
        redisTemplate.delete(sessionKey(userId, sessionId));
        redisTemplate.opsForSet().remove(userSetKey(userId), sessionId);
    }

    public boolean existsById(Long userId, String sessionId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(sessionKey(userId, sessionId)));
    }

    private String sessionKey(Long userId, String sessionId) {
        return SESSION_KEY_PREFIX + userId + ":" + sessionId;
    }

    private String userSetKey(Long userId) {
        return USER_SESSIONS_KEY_PREFIX + userId;
    }
}
