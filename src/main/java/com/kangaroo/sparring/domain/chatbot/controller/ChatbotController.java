package com.kangaroo.sparring.domain.chatbot.controller;

import com.kangaroo.sparring.domain.chatbot.dto.req.ChatMessageRequest;
import com.kangaroo.sparring.domain.chatbot.dto.req.CreateSessionRequest;
import com.kangaroo.sparring.domain.chatbot.dto.res.ChatSessionListItemResponse;
import com.kangaroo.sparring.domain.chatbot.dto.res.ChatSessionResponse;
import com.kangaroo.sparring.domain.chatbot.service.ChatbotService;
import com.kangaroo.sparring.global.response.MessageResponse;
import com.kangaroo.sparring.global.security.principal.PrincipalResolver;
import com.kangaroo.sparring.global.security.principal.UserIdPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@Tag(name = "챗봇", description = "AI 건강 어시스턴트 챗봇 API")
@Slf4j
@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    @Operation(summary = "세션 생성", description = "새로운 챗봇 대화 세션을 생성합니다.")
    @PostMapping("/sessions")
    public ResponseEntity<ChatSessionResponse> createSession(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @Valid @RequestBody(required = false) CreateSessionRequest request
    ) {
        Long userId = PrincipalResolver.resolveUserId(principal);
        CreateSessionRequest req = request != null ? request : new CreateSessionRequest();
        return ResponseEntity.status(HttpStatus.CREATED).body(chatbotService.createSession(userId, req));
    }

    @Operation(summary = "세션 목록 조회", description = "사용자의 챗봇 대화 세션 목록을 최신순으로 조회합니다.")
    @GetMapping("/sessions")
    public ResponseEntity<List<ChatSessionListItemResponse>> listSessions(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @RequestParam(required = false) Integer limit
    ) {
        Long userId = PrincipalResolver.resolveUserId(principal);
        return ResponseEntity.ok(chatbotService.listSessions(userId, limit));
    }

    @Operation(summary = "세션 상세 조회", description = "특정 세션의 전체 대화 내용을 조회합니다.")
    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<ChatSessionResponse> getSession(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @PathVariable String sessionId
    ) {
        Long userId = PrincipalResolver.resolveUserId(principal);
        return ResponseEntity.ok(chatbotService.getSession(userId, sessionId));
    }

    @Operation(summary = "세션 삭제", description = "특정 챗봇 대화 세션을 삭제합니다.")
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<MessageResponse> deleteSession(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @PathVariable String sessionId
    ) {
        Long userId = PrincipalResolver.resolveUserId(principal);
        chatbotService.deleteSession(userId, sessionId);
        return ResponseEntity.ok(MessageResponse.of("대화 세션이 삭제되었습니다."));
    }

    @Operation(summary = "챗봇 메시지 스트리밍", description = "메시지 전송 후 AI 응답 스트리밍 수신")
    @PostMapping(value = "/sessions/{sessionId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @PathVariable String sessionId,
            @Valid @RequestBody ChatMessageRequest request
    ) {
        Long userId = PrincipalResolver.resolveUserId(principal);
        log.info("챗봇 스트리밍 요청: userId={}, sessionId={}", userId, sessionId);
        return chatbotService.streamChat(userId, sessionId, request);
    }
}
