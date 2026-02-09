package com.kangaroo.sparring.domain.user.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class OAuth2CodeRequest {

    @Schema(description = "OAuth2 authorization code", example = "4/0AfJohXk...")
    private String code;

    @Schema(description = "소셜 SDK 액세스 토큰(카카오 등)", example = "eyJhbGciOi...")
    private String accessToken;
}
