package com.kangaroo.sparring.domain.chatbot.service;

import com.kangaroo.sparring.domain.chatbot.dto.req.ChatMessageRequest;
import com.kangaroo.sparring.domain.chatbot.dto.req.CreateSessionRequest;
import com.kangaroo.sparring.domain.chatbot.dto.res.ChatSessionResponse;
import com.kangaroo.sparring.domain.chatbot.session.ChatMessage;
import com.kangaroo.sparring.domain.chatbot.session.ChatSession;
import com.kangaroo.sparring.domain.chatbot.session.ChatSessionRepository;
import com.kangaroo.sparring.domain.chatbot.type.MessageRole;
import com.kangaroo.sparring.domain.healthprofile.entity.HealthProfile;
import com.kangaroo.sparring.domain.healthprofile.repository.HealthProfileRepository;
import com.kangaroo.sparring.domain.measurement.entity.BloodPressureLog;
import com.kangaroo.sparring.domain.measurement.entity.BloodSugarLog;
import com.kangaroo.sparring.domain.measurement.repository.BloodPressureLogRepository;
import com.kangaroo.sparring.domain.measurement.repository.BloodSugarLogRepository;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotService {

    private final ChatSessionRepository sessionRepository;
    private final GeminiStreamingClient geminiStreamingClient;
    private final HealthProfileRepository healthProfileRepository;
    private final BloodPressureLogRepository bloodPressureLogRepository;
    private final BloodSugarLogRepository bloodSugarLogRepository;

    public ChatSessionResponse createSession(Long userId, CreateSessionRequest request) {
        String sessionId = UUID.randomUUID().toString();
        String title = (request.getTitle() != null && !request.getTitle().isBlank())
                ? request.getTitle()
                : "새 대화";

        ChatSession session = ChatSession.builder()
                .sessionId(sessionId)
                .userId(userId)
                .title(title)
                .messages(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .lastActiveAt(LocalDateTime.now())
                .build();

        sessionRepository.save(session);
        return ChatSessionResponse.from(session);
    }

    public ChatSessionResponse getSession(Long userId, String sessionId) {
        return ChatSessionResponse.from(findSessionOrThrow(userId, sessionId));
    }

    public List<ChatSessionResponse> listSessions(Long userId) {
        return sessionRepository.findAllByUserId(userId)
                .stream()
                .map(ChatSessionResponse::from)
                .toList();
    }

    public void deleteSession(Long userId, String sessionId) {
        findSessionOrThrow(userId, sessionId);
        sessionRepository.delete(userId, sessionId);
    }

    /**
     * 사용자 메시지를 세션에 저장하고, Gemini 스트리밍 응답을 SseEmitter로 전달한다.
     * 스트리밍 완료 후 모델 응답 전체를 Redis 세션에 저장한다.
     */
    public SseEmitter streamChat(Long userId, String sessionId, ChatMessageRequest request) {
        ChatSession session = findSessionOrThrow(userId, sessionId);
        ChatSession updatedSession = appendUserMessage(session, request.getMessage());
        sessionRepository.save(updatedSession);

        // SseEmitter: 3분 타임아웃 (느린 Gemini 응답 고려)
        SseEmitter emitter = new SseEmitter(180_000L);
        AtomicBoolean completed = new AtomicBoolean(false);
        AtomicReference<Disposable> subscriptionRef = new AtomicReference<>();
        StringBuilder fullResponse = new StringBuilder();
        String userContextSummary = buildUserContextSummary(userId);
        log.debug("챗봇 컨텍스트 포함 여부: included={}, length={}",
                !userContextSummary.isBlank(),
                userContextSummary.length());

        Disposable subscription = geminiStreamingClient.streamChat(updatedSession.getMessages(), userContextSummary)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(
                        // onNext: 토큰 청크를 클라이언트로 전송
                        chunk -> {
                            if (completed.get()) return;
                            try {
                                fullResponse.append(chunk);
                                emitter.send(SseEmitter.event().data(chunk).build());
                            } catch (IOException e) {
                                log.warn("SSE 전송 실패 (클라이언트 연결 해제): {}", e.getMessage());
                                if (completed.compareAndSet(false, true)) {
                                    disposeSubscription(subscriptionRef);
                                    emitter.completeWithError(e);
                                }
                            }
                        },
                        // onError: 오류 이벤트 전송 후 종료
                        error -> {
                            if (completed.compareAndSet(false, true)) {
                                try {
                                    String errorMsg = error instanceof CustomException ce
                                            ? ce.getErrorCode().getMessage()
                                            : "AI 응답 중 오류가 발생했습니다.";
                                    emitter.send(SseEmitter.event().name("error").data(errorMsg).build());
                                } catch (IOException ignored) {}
                                disposeSubscription(subscriptionRef);
                                emitter.completeWithError(error);
                            }
                        },
                        // onComplete: 모델 응답 저장 후 완료 신호 전송
                        () -> {
                            if (completed.compareAndSet(false, true)) {
                                persistModelReply(updatedSession, userId, fullResponse.toString());
                                try {
                                    emitter.send(SseEmitter.event().name("done").data("[DONE]").build());
                                } catch (IOException e) {
                                    log.warn("[DONE] 이벤트 전송 실패: {}", e.getMessage());
                                } finally {
                                    disposeSubscription(subscriptionRef);
                                    emitter.complete();
                                }
                            }
                        }
                );
        subscriptionRef.set(subscription);

        emitter.onTimeout(() -> {
            if (completed.compareAndSet(false, true)) {
                disposeSubscription(subscriptionRef);
                emitter.complete();
            }
        });
        emitter.onError(ex -> {
            completed.set(true);
            disposeSubscription(subscriptionRef);
        });
        emitter.onCompletion(() -> disposeSubscription(subscriptionRef));

        return emitter;
    }

    private ChatSession appendUserMessage(ChatSession session, String message) {
        List<ChatMessage> messages = new ArrayList<>(session.getMessages());
        messages.add(ChatMessage.builder()
                .role(MessageRole.USER)
                .content(message)
                .timestamp(LocalDateTime.now())
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
                .lastActiveAt(LocalDateTime.now())
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

        List<ChatMessage> messages = new ArrayList<>(session.getMessages());
        messages.add(ChatMessage.builder()
                .role(MessageRole.MODEL)
                .content(fullText)
                .timestamp(LocalDateTime.now())
                .build());

        ChatSession finalSession = ChatSession.builder()
                .sessionId(session.getSessionId())
                .userId(userId)
                .title(session.getTitle())
                .messages(messages)
                .createdAt(session.getCreatedAt())
                .lastActiveAt(LocalDateTime.now())
                .build();

        sessionRepository.save(finalSession);
    }

    private ChatSession findSessionOrThrow(Long userId, String sessionId) {
        return sessionRepository.findById(userId, sessionId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHATBOT_SESSION_NOT_FOUND));
    }

    private String buildUserContextSummary(Long userId) {
        StringBuilder sb = new StringBuilder();
        sb.append("[사용자 건강 컨텍스트]\n");

        HealthProfile profile = healthProfileRepository.findByUserId(userId).orElse(null);
        if (profile != null) {
            if (profile.getBirthDate() != null) {
                int age = Period.between(profile.getBirthDate(), LocalDate.now()).getYears();
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

        List<BloodPressureLog> bloodPressureLogs = bloodPressureLogRepository.findRecentByUserId(
                userId,
                PageRequest.of(0, 3)
        );
        if (!bloodPressureLogs.isEmpty()) {
            List<String> recentBp = bloodPressureLogs.stream()
                    .map(log -> log.getSystolic() + "/" + log.getDiastolic())
                    .toList();
            sb.append("- 최근 혈압(최신순): ").append(String.join(", ", recentBp)).append("\n");
        }

        List<BloodSugarLog> bloodSugarLogs = bloodSugarLogRepository.findRecentByUserId(
                userId,
                PageRequest.of(0, 3)
        );
        if (!bloodSugarLogs.isEmpty()) {
            List<String> recentSugar = bloodSugarLogs.stream()
                    .map(log -> log.getGlucoseLevel() + " mg/dL")
                    .toList();
            sb.append("- 최근 혈당(최신순): ").append(String.join(", ", recentSugar)).append("\n");
        }

        if (sb.toString().equals("[사용자 건강 컨텍스트]\n")) {
            return "";
        }
        sb.append("- 기준 시각: ").append(LocalDateTime.now()).append("\n");
        return sb.toString();
    }
}
