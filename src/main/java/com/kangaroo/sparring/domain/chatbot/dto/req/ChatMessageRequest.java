package com.kangaroo.sparring.domain.chatbot.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "챗봇 메시지 전송 요청")
public class ChatMessageRequest {

    @Schema(description = "사용자 메시지", example = "혈압이 140/90이면 고혈압인가요?")
    @NotBlank(message = "메시지는 비워둘 수 없습니다")
    @Size(max = 2000, message = "메시지는 2000자 이내로 입력해주세요")
    private String message;
}
