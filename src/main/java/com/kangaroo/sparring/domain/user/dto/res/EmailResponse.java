package com.kangaroo.sparring.domain.user.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "이메일 인증 응답")
public class EmailResponse {

    @Schema(description = "이메일", example = "test@example.com")
    private String email;

    @Schema(description = "메시지", example = "인증코드가 발송되었습니다.")
    private String message;

    @Schema(description = "인증 요청 ID", example = "4b2f6b2d9c4f4a9e9bb1d2f0e4b1c2a3")
    private String verificationId;

    @Schema(description = "만료 시각", example = "2026-02-04T12:30:00")
    private LocalDateTime expiresAt;

    public static EmailResponse of(String email, String message) {
        return EmailResponse.builder()
                .email(email)
                .message(message)
                .build();
    }

    public static EmailResponse withVerification(String email, String message, String verificationId, LocalDateTime expiresAt) {
        return EmailResponse.builder()
                .email(email)
                .message(message)
                .verificationId(verificationId)
                .expiresAt(expiresAt)
                .build();
    }
}
