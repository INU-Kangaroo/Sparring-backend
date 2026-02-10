package com.kangaroo.sparring.domain.user.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import lombok.Getter;

@Getter
public class OAuth2CodeRequest {

    @Schema(description = "OAuth2 authorization code", example = "4/0AfJohXk...")
    private String code;

    @Schema(description = "OAuth2 redirect URI (authorization code 교환 시 필요)", example = "com.example.app:/oauth2redirect")
    private String redirectUri;

    @Schema(description = "PKCE code_verifier (authorization code 교환 시 필요)", example = "hJtX...sK9")
    private String codeVerifier;

    @Schema(description = "소셜 SDK 액세스 토큰(카카오 등)", example = "eyJhbGciOi...")
    private String accessToken;

    @AssertTrue(message = "code 또는 accessToken 중 하나는 필수입니다.")
    private boolean isValidRequest() {
        boolean hasCode = code != null && !code.isBlank();
        boolean hasAccessToken = accessToken != null && !accessToken.isBlank();
        return hasCode || hasAccessToken;
    }
}
