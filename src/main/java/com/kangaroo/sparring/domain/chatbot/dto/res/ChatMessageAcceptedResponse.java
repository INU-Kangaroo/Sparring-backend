package com.kangaroo.sparring.domain.chatbot.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "챗봇 메시지 등록 응답")
public class ChatMessageAcceptedResponse {

    @Schema(description = "등록된 메시지 ID")
    private String messageId;

    @Schema(description = "SSE 스트림 URL", example = "/api/chatbot/streams/1d6c2f8f-8b30-4ca8-8f7a-0af7e0f8d6f2")
    private String streamUrl;

    public static ChatMessageAcceptedResponse of(String messageId, String streamUrl) {
        return ChatMessageAcceptedResponse.builder()
                .messageId(messageId)
                .streamUrl(streamUrl)
                .build();
    }
}
