package com.kangaroo.sparring.domain.user.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "이메일 인증 응답")
public class EmailResponse {

    @Schema(description = "이메일")
    private String email;

    @Schema(description = "메시지")
    private String message;

    public static EmailResponse of(String email, String message) {
        return EmailResponse.builder()
                .email(email)
                .message(message)
                .build();
    }
}