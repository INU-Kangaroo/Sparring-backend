package com.kangaroo.sparring.domain.user.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "이메일 인증코드 검증 요청")
public class VerifyCodeRequest {

    @NotBlank(message = "verificationId는 필수입니다")
    @Schema(description = "인증 요청 ID", example = "4b2f6b2d9c4f4a9e9bb1d2f0e4b1c2a3")
    private String verificationId;

    @NotBlank(message = "인증코드는 필수입니다")
    @Schema(description = "인증코드", example = "123456")
    private String code;
}
