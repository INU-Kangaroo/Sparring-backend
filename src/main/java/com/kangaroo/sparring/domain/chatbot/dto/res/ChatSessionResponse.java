package com.kangaroo.sparring.domain.chatbot.dto.res;

import com.kangaroo.sparring.domain.chatbot.entity.ChatSession;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "챗봇 세션 응답")
public class ChatSessionResponse {

    @Schema(description = "세션 ID")
    private String sessionId;

    @Schema(description = "세션 제목")
    private String title;

    @Schema(description = "대화 메시지 목록")
    private List<ChatMessageDto> messages;

    @Schema(description = "세션 생성 시각")
    private LocalDateTime createdAt;

    @Schema(description = "마지막 활동 시각")
    private LocalDateTime lastActiveAt;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "대화 메시지")
    public static class ChatMessageDto {

        @Schema(description = "발화자 역할 (USER / MODEL)")
        private String role;

        @Schema(description = "메시지 내용")
        private String content;

        @Schema(description = "메시지 시각")
        private LocalDateTime timestamp;
    }

    public static ChatSessionResponse from(ChatSession session) {
        List<ChatMessageDto> messageDtos = session.getMessages().stream()
                .map(m -> ChatMessageDto.builder()
                        .role(m.getRole().name())
                        .content(m.getContent())
                        .timestamp(m.getTimestamp())
                        .build())
                .toList();

        return ChatSessionResponse.builder()
                .sessionId(session.getSessionId())
                .title(session.getTitle())
                .messages(messageDtos)
                .createdAt(session.getCreatedAt())
                .lastActiveAt(session.getLastActiveAt())
                .build();
    }
}
