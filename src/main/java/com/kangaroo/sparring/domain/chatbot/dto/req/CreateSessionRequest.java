package com.kangaroo.sparring.domain.chatbot.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "챗봇 세션 생성 요청")
public class CreateSessionRequest {

    @Schema(description = "세션 제목 (선택, 미입력 시 첫 메시지로 자동 설정)", example = "혈압 관리 문의")
    @Size(max = 100, message = "세션 제목은 100자 이내로 입력해주세요")
    private String title;
}
