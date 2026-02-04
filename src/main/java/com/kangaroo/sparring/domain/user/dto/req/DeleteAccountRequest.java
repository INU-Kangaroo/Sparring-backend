package com.kangaroo.sparring.domain.user.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "회원 탈퇴 요청")
public class DeleteAccountRequest {

    @Schema(description = "비밀번호 (LOCAL 계정만 필수)", example = "password123!")
    private String password;
}
