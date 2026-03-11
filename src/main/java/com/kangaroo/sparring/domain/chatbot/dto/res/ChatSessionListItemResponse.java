package com.kangaroo.sparring.domain.chatbot.dto.res;

import com.kangaroo.sparring.domain.chatbot.session.ChatMessage;
import com.kangaroo.sparring.domain.chatbot.session.ChatSession;
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
@Schema(description = "챗봇 세션 목록 아이템")
public class ChatSessionListItemResponse {

    private static final int PREVIEW_MAX_LENGTH = 80;

    @Schema(description = "세션 ID")
    private String sessionId;

    @Schema(description = "세션 제목")
    private String title;

    @Schema(description = "마지막 메시지 미리보기")
    private String preview;

    @Schema(description = "메시지 개수")
    private Integer messageCount;

    @Schema(description = "마지막 활동 시각")
    private LocalDateTime lastActiveAt;

    public static ChatSessionListItemResponse from(ChatSession session) {
        List<ChatMessage> messages = session.getMessages();
        int count = messages == null ? 0 : messages.size();
        String preview = count == 0 ? null : abbreviate(messages.get(count - 1).getContent());

        return ChatSessionListItemResponse.builder()
                .sessionId(session.getSessionId())
                .title(session.getTitle())
                .preview(preview)
                .messageCount(count)
                .lastActiveAt(session.getLastActiveAt())
                .build();
    }

    private static String abbreviate(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }
        String normalized = content.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= PREVIEW_MAX_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, PREVIEW_MAX_LENGTH) + "...";
    }
}
