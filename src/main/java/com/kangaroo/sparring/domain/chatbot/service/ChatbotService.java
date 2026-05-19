package com.kangaroo.sparring.domain.chatbot.service;

import com.kangaroo.sparring.domain.chatbot.dto.req.ChatMessageRequest;
import com.kangaroo.sparring.domain.chatbot.dto.req.CreateSessionRequest;
import com.kangaroo.sparring.domain.chatbot.dto.res.ChatMessageAcceptedResponse;
import com.kangaroo.sparring.domain.chatbot.dto.res.ChatSessionListItemResponse;
import com.kangaroo.sparring.domain.chatbot.dto.res.ChatSessionResponse;
import com.kangaroo.sparring.domain.chatbot.client.GeminiStreamingClient;
import com.kangaroo.sparring.domain.chatbot.entity.ChatMessage;
import com.kangaroo.sparring.domain.chatbot.entity.ChatSession;
import com.kangaroo.sparring.domain.chatbot.repository.ChatSessionRepository;
import com.kangaroo.sparring.domain.chatbot.type.MessageRole;
import com.kangaroo.sparring.domain.healthprofile.entity.HealthProfile;
import com.kangaroo.sparring.domain.healthprofile.repository.HealthProfileRepository;
import com.kangaroo.sparring.domain.record.common.BloodPressureRecord;
import com.kangaroo.sparring.domain.record.common.BloodSugarRecord;
import com.kangaroo.sparring.domain.record.common.RecordReadService;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotService {
    public static final int DEFAULT_SESSION_LIST_LIMIT = 30;
    public static final int MAX_SESSION_LIST_LIMIT = 100;
    private static final long STREAM_TIMEOUT_MILLIS = 180_000L;
    private static final Duration STREAM_CONTEXT_TTL = Duration.ofMinutes(10);

    private final ChatSessionRepository sessionRepository;
    private final GeminiStreamingClient geminiStreamingClient;
    private final HealthProfileRepository healthProfileRepository;
    private final RecordReadService recordReadService;
    private final Clock kstClock;
    private final ConcurrentMap<String, PendingStreamContext> pendingStreams = new ConcurrentHashMap<>();

    public ChatSessionResponse createSession(Long userId, CreateSessionRequest request) {
        LocalDateTime now = now();
        String sessionId = UUID.randomUUID().toString();
        String title = (request.getTitle() != null && !request.getTitle().isBlank())
                ? request.getTitle()
                : "새 대화";

        ChatSession session = ChatSession.builder()
                .sessionId(sessionId)
                .userId(userId)
                .title(title)
                .messages(new ArrayList<>())
                .createdAt(now)
                .lastActiveAt(now)
                .build();

        sessionRepository.save(session);
        return ChatSessionResponse.from(session);
    }

    public ChatSessionResponse getSession(Long userId, String sessionId) {
        return ChatSessionResponse.from(findSessionOrThrow(userId, sessionId));
    }

    public List<ChatSessionListItemResponse> listSessions(Long userId, Integer limit) {
        int validatedLimit = validateSessionListLimit(limit);
        return sessionRepository.findRecentByUserId(userId, validatedLimit)
                .stream()
                .map(ChatSessionListItemResponse::from)
                .toList();
    }

    public void deleteSession(Long userId, String sessionId) {
        findSessionOrThrow(userId, sessionId);
        sessionRepository.delete(userId, sessionId);
    }

    public ChatMessageAcceptedResponse createMessage(Long userId, String sessionId, ChatMessageRequest request) {
        evictExpiredPendingStreams();
        ChatSession session = findSessionOrThrow(userId, sessionId);
        ChatSession updatedSession = appendUserMessage(session, request.getMessage());
        sessionRepository.save(updatedSession);
        String messageId = UUID.randomUUID().toString();
        pendingStreams.put(messageId, new PendingStreamContext(messageId, userId, updatedSession, now()));
        return ChatMessageAcceptedResponse.of(messageId, "/api/chatbot/streams/" + messageId);
    }

    /**
     * Deprecated 흐름: POST 한 번으로 메시지 저장 + SSE 스트리밍을 동시에 처리한다.
     */
    public SseEmitter streamChat(Long userId, String sessionId, ChatMessageRequest request) {
        ChatMessageAcceptedResponse accepted = createMessage(userId, sessionId, request);
        return streamByMessageId(userId, accepted.getMessageId());
    }

    public SseEmitter streamByMessageId(Long userId, String messageId) {
        evictExpiredPendingStreams();
        PendingStreamContext streamContext = findPendingStreamOrThrow(userId, messageId);
        if (!streamContext.started.compareAndSet(false, true)) {
            throw new CustomException(ErrorCode.CHATBOT_STREAM_NOT_FOUND);
        }

        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MILLIS);
        streamContext.emitterRef.set(emitter);
        AtomicBoolean completed = new AtomicBoolean(false);
        StringBuilder fullResponse = new StringBuilder();
        String userContextSummary = buildUserContextSummary(userId);
        log.debug("챗봇 컨텍스트 포함 여부: included={}, length={}",
                !userContextSummary.isBlank(),
                userContextSummary.length());

        Disposable subscription = geminiStreamingClient.streamChat(streamContext.updatedSession.getMessages(), userContextSummary)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(
                        // onNext: 토큰 청크를 클라이언트로 전송
                        chunk -> handleStreamChunk(emitter, completed, streamContext.subscriptionRef, fullResponse, chunk),
                        // onError: 오류 이벤트 전송 후 종료
                        error -> handleStreamError(emitter, completed, streamContext.subscriptionRef, streamContext.messageId, error),
                        // onComplete: 모델 응답 저장 후 완료 신호 전송
                        () -> handleStreamComplete(
                                emitter,
                                completed,
                                streamContext.subscriptionRef,
                                streamContext.messageId,
                                streamContext.updatedSession,
                                userId,
                                fullResponse
                        )
                );
        streamContext.subscriptionRef.set(subscription);

        emitter.onTimeout(() -> {
            if (completed.compareAndSet(false, true)) {
                disposeSubscription(streamContext.subscriptionRef);
                cleanupPendingStream(streamContext.messageId);
                emitter.complete();
            }
        });
        emitter.onError(ex -> {
            completed.set(true);
            disposeSubscription(streamContext.subscriptionRef);
            cleanupPendingStream(streamContext.messageId);
        });
        emitter.onCompletion(() -> {
            disposeSubscription(streamContext.subscriptionRef);
            cleanupPendingStream(streamContext.messageId);
        });

        return emitter;
    }

    public void cancelStream(Long userId, String messageId) {
        PendingStreamContext streamContext = findPendingStreamOrThrow(userId, messageId);
        disposeSubscription(streamContext.subscriptionRef);
        SseEmitter emitter = streamContext.emitterRef.get();
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().name("error").data(Map.of("message", "스트림이 중단되었습니다.")).build());
            } catch (IOException ignored) {
            } finally {
                emitter.complete();
            }
        }
        cleanupPendingStream(messageId);
    }

    private ChatSession appendUserMessage(ChatSession session, String message) {
        LocalDateTime now = now();
        List<ChatMessage> messages = new ArrayList<>(session.getMessages());
        messages.add(ChatMessage.builder()
                .role(MessageRole.USER)
                .content(message)
                .timestamp(now)
                .build());

        String title = session.getTitle();
        if ("새 대화".equals(title) && messages.size() == 1) {
            title = message.length() > 50 ? message.substring(0, 50) + "..." : message;
        }

        return ChatSession.builder()
                .sessionId(session.getSessionId())
                .userId(session.getUserId())
                .title(title)
                .messages(messages)
                .createdAt(session.getCreatedAt())
                .lastActiveAt(now)
                .build();
    }

    private void disposeSubscription(AtomicReference<Disposable> subscriptionRef) {
        Disposable disposable = subscriptionRef.get();
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
    }

    private void persistModelReply(ChatSession session, Long userId, String fullText) {
        if (fullText.isBlank()) return;

        LocalDateTime now = now();
        ChatSession latestSession = sessionRepository.findById(userId, session.getSessionId()).orElse(session);

        List<ChatMessage> messages = new ArrayList<>(latestSession.getMessages());
        messages.add(ChatMessage.builder()
                .role(MessageRole.MODEL)
                .content(fullText)
                .timestamp(now)
                .build());

        ChatSession finalSession = ChatSession.builder()
                .sessionId(latestSession.getSessionId())
                .userId(userId)
                .title(latestSession.getTitle())
                .messages(messages)
                .createdAt(latestSession.getCreatedAt())
                .lastActiveAt(now)
                .build();

        sessionRepository.save(finalSession);
    }

    private ChatSession findSessionOrThrow(Long userId, String sessionId) {
        return sessionRepository.findById(userId, sessionId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHATBOT_SESSION_NOT_FOUND));
    }

    private int validateSessionListLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_SESSION_LIST_LIMIT;
        }
        if (limit <= 0 || limit > MAX_SESSION_LIST_LIMIT) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        return limit;
    }

    private String buildUserContextSummary(Long userId) {
        StringBuilder sb = new StringBuilder();
        sb.append("[사용자 건강 컨텍스트]\n");

        HealthProfile profile = healthProfileRepository.findByUserId(userId).orElse(null);
        if (profile != null) {
            if (profile.getBirthDate() != null) {
                int age = Period.between(profile.getBirthDate(), today()).getYears();
                sb.append("- 나이: ").append(age).append("세\n");
            }
            if (profile.getGender() != null) {
                sb.append("- 성별: ").append(profile.getGender()).append("\n");
            }
            if (profile.getBloodPressureStatus() != null) {
                sb.append("- 혈압 상태: ").append(profile.getBloodPressureStatus()).append("\n");
            }
            if (profile.getBloodSugarStatus() != null) {
                sb.append("- 혈당 상태: ").append(profile.getBloodSugarStatus()).append("\n");
            }
            if (profile.getMedications() != null && !profile.getMedications().isBlank()) {
                sb.append("- 복용약: ").append(profile.getMedications()).append("\n");
            }
            if (profile.getHealthGoal() != null && !profile.getHealthGoal().isBlank()) {
                sb.append("- 건강 목표: ").append(profile.getHealthGoal()).append("\n");
            }
        }

        List<BloodPressureRecord> bloodPressureLogs = recordReadService.getRecentBloodPressureRecords(userId, 3);
        if (!bloodPressureLogs.isEmpty()) {
            List<String> recentBp = bloodPressureLogs.stream()
                    .map(log -> log.getSystolic() + "/" + log.getDiastolic())
                    .toList();
            sb.append("- 최근 혈압(최신순): ").append(String.join(", ", recentBp)).append("\n");
        }

        List<BloodSugarRecord> bloodSugarLogs = recordReadService.getRecentBloodSugarRecords(userId, 3);
        if (!bloodSugarLogs.isEmpty()) {
            List<String> recentSugar = bloodSugarLogs.stream()
                    .map(log -> log.getGlucoseLevel() + " mg/dL")
                    .toList();
            sb.append("- 최근 혈당(최신순): ").append(String.join(", ", recentSugar)).append("\n");
        }

        if (sb.toString().equals("[사용자 건강 컨텍스트]\n")) {
            return "";
        }
        sb.append("- 기준 시각: ").append(now()).append("\n");
        return sb.toString();
    }

    private void handleStreamChunk(
            SseEmitter emitter,
            AtomicBoolean completed,
            AtomicReference<Disposable> subscriptionRef,
            StringBuilder fullResponse,
            String chunk
    ) {
        if (completed.get()) {
            return;
        }
        try {
            fullResponse.append(chunk);
            emitter.send(SseEmitter.event()
                    .name("token")
                    .data(Map.of(
                            "delta", chunk,
                            "accumulated", fullResponse.toString()
                    ))
                    .build());
        } catch (IOException e) {
            log.warn("SSE 전송 실패 (클라이언트 연결 해제): {}", e.getMessage());
            if (completed.compareAndSet(false, true)) {
                disposeSubscription(subscriptionRef);
                emitter.completeWithError(e);
            }
        }
    }

    private void handleStreamError(
            SseEmitter emitter,
            AtomicBoolean completed,
            AtomicReference<Disposable> subscriptionRef,
            String messageId,
            Throwable error
    ) {
        if (completed.compareAndSet(false, true)) {
            try {
                String errorMsg = error instanceof CustomException ce
                        ? ce.getErrorCode().getMessage()
                        : "AI 응답 중 오류가 발생했습니다.";
                emitter.send(SseEmitter.event().name("error").data(Map.of("message", errorMsg)).build());
            } catch (IOException ignored) {
            }
            disposeSubscription(subscriptionRef);
            cleanupPendingStream(messageId);
            emitter.completeWithError(error);
        }
    }

    private void handleStreamComplete(
            SseEmitter emitter,
            AtomicBoolean completed,
            AtomicReference<Disposable> subscriptionRef,
            String messageId,
            ChatSession session,
            Long userId,
            StringBuilder fullResponse
    ) {
        if (completed.compareAndSet(false, true)) {
            persistModelReply(session, userId, fullResponse.toString());
            try {
                emitter.send(SseEmitter.event().name("done").data(Map.of("messageId", messageId)).build());
            } catch (IOException e) {
                log.warn("[DONE] 이벤트 전송 실패: {}", e.getMessage());
            } finally {
                disposeSubscription(subscriptionRef);
                cleanupPendingStream(messageId);
                emitter.complete();
            }
        }
    }

    private PendingStreamContext findPendingStreamOrThrow(Long userId, String messageId) {
        PendingStreamContext streamContext = pendingStreams.get(messageId);
        if (streamContext == null) {
            throw new CustomException(ErrorCode.CHATBOT_STREAM_NOT_FOUND);
        }
        if (!streamContext.userId.equals(userId)) {
            throw new CustomException(ErrorCode.CHATBOT_STREAM_NOT_FOUND);
        }
        if (isExpired(streamContext.createdAt)) {
            cleanupPendingStream(messageId);
            throw new CustomException(ErrorCode.CHATBOT_STREAM_NOT_FOUND);
        }
        return streamContext;
    }

    private void evictExpiredPendingStreams() {
        pendingStreams.forEach((messageId, context) -> {
            if (isExpired(context.createdAt)) {
                disposeSubscription(context.subscriptionRef);
                cleanupPendingStream(messageId);
            }
        });
    }

    private boolean isExpired(LocalDateTime createdAt) {
        return createdAt.plus(STREAM_CONTEXT_TTL).isBefore(now());
    }

    private void cleanupPendingStream(String messageId) {
        pendingStreams.remove(messageId);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(kstClock);
    }

    private LocalDate today() {
        return LocalDate.now(kstClock);
    }

    private static final class PendingStreamContext {
        private final String messageId;
        private final Long userId;
        private final ChatSession updatedSession;
        private final LocalDateTime createdAt;
        private final AtomicBoolean started = new AtomicBoolean(false);
        private final AtomicReference<Disposable> subscriptionRef = new AtomicReference<>();
        private final AtomicReference<SseEmitter> emitterRef = new AtomicReference<>();

        private PendingStreamContext(String messageId, Long userId, ChatSession updatedSession, LocalDateTime createdAt) {
            this.messageId = messageId;
            this.userId = userId;
            this.updatedSession = updatedSession;
            this.createdAt = createdAt;
        }
    }
}
