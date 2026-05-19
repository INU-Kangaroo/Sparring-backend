package com.kangaroo.sparring.domain.auth.controller;

import com.kangaroo.sparring.domain.auth.dto.req.EmailRequest;
import com.kangaroo.sparring.domain.auth.dto.req.GoogleSdkLoginRequest;
import com.kangaroo.sparring.domain.auth.dto.req.KakaoSdkLoginRequest;
import com.kangaroo.sparring.domain.auth.dto.req.LoginRequest;
import com.kangaroo.sparring.domain.auth.dto.req.OAuth2PkceLoginRequest;
import com.kangaroo.sparring.domain.auth.dto.req.SignupRequest;
import com.kangaroo.sparring.domain.auth.dto.req.VerifyCodeRequest;
import com.kangaroo.sparring.domain.auth.dto.res.AuthResponse;
import com.kangaroo.sparring.domain.auth.dto.res.EmailResponse;
import com.kangaroo.sparring.domain.auth.service.AuthTokenService;
import com.kangaroo.sparring.domain.auth.service.EmailService;
import com.kangaroo.sparring.domain.auth.service.EmailVerificationResult;
import com.kangaroo.sparring.domain.auth.service.UserRegistrationService;
import com.kangaroo.sparring.domain.user.service.UserAccountService;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import com.kangaroo.sparring.global.response.MessageResponse;
import com.kangaroo.sparring.global.security.oauth2.service.OAuth2CodeAuthService;
import com.kangaroo.sparring.global.security.principal.UserIdPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.function.Supplier;

@Tag(name = "인증", description = "회원가입 및 로그인 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final UserRegistrationService userRegistrationService;
    private final UserAccountService userAccountService;
    private final AuthTokenService authTokenService;
    private final EmailService emailService;
    private final OAuth2CodeAuthService oAuth2CodeAuthService;

    @PostMapping("/send-verification")
    @Operation(summary = "이메일 인증코드 발송", description = "회원가입/이메일 변경용 인증코드 발송")
    public ResponseEntity<EmailResponse> sendVerificationCode(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @Valid @RequestBody EmailRequest request
    ) {
        Long userId = principal != null ? principal.getUserId() : null;
        EmailVerificationResult result = emailService.sendVerificationCode(request.getEmail(), userId);
        return toVerificationResponse(result, "인증코드가 발송되었습니다.");
    }

    @PostMapping("/resend-verification")
    @Operation(summary = "이메일 인증코드 재발송", description = "인증코드 재발송 (1분 쿨다운)")
    public ResponseEntity<EmailResponse> resendVerificationCode(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @Valid @RequestBody EmailRequest request
    ) {
        Long userId = principal != null ? principal.getUserId() : null;
        EmailVerificationResult result = emailService.resendVerificationCode(request.getEmail(), userId);
        return toVerificationResponse(result, "인증코드가 재발송되었습니다.");
    }

    @PostMapping("/verify-code")
    @Operation(summary = "인증코드 검증", description = "verificationId + code 검증")
    public ResponseEntity<EmailResponse> verifyCode(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @Valid @RequestBody VerifyCodeRequest request
    ) {
        EmailVerificationResult result = emailService.verifyCode(request.getVerificationId(), request.getCode());

        if (result.getUserId() != null) {
            if (principal == null || !result.getUserId().equals(principal.getUserId())) {
                throw new CustomException(ErrorCode.UNAUTHORIZED);
            }
            userAccountService.updateEmail(result.getUserId(), result.getEmail());
            return ResponseEntity.ok(EmailResponse.of(result.getEmail(), "이메일 변경이 완료되었습니다."));
        }

        return ResponseEntity.ok(EmailResponse.of(result.getEmail(), "이메일 인증이 완료되었습니다."));
    }

    @PostMapping("/signup")
    @Operation(summary = "회원가입", description = "이메일/비밀번호 회원가입")
    public ResponseEntity<MessageResponse> signup(@Valid @RequestBody SignupRequest request) {
        userRegistrationService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(MessageResponse.of("회원가입이 완료되었습니다."));
    }

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "이메일/비밀번호 로그인")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authTokenService.login(request));
    }

    @PostMapping("/oauth2/google/pkce")
    @Operation(summary = "Google PKCE 로그인")
    public ResponseEntity<AuthResponse> googlePkceLogin(@Valid @RequestBody OAuth2PkceLoginRequest request) {
        return oauthLogin(() -> oAuth2CodeAuthService.loginWithGooglePkce(request));
    }

    @PostMapping("/oauth2/google/sdk")
    @Operation(summary = "Google SDK 로그인")
    public ResponseEntity<AuthResponse> googleSdkLogin(@Valid @RequestBody GoogleSdkLoginRequest request) {
        return oauthLogin(() -> oAuth2CodeAuthService.loginWithGoogleSdkCode(request));
    }

    @PostMapping("/oauth2/kakao/pkce")
    @Operation(summary = "Kakao PKCE 로그인")
    public ResponseEntity<AuthResponse> kakaoPkceLogin(@Valid @RequestBody OAuth2PkceLoginRequest request) {
        return oauthLogin(() -> oAuth2CodeAuthService.loginWithKakaoPkce(request));
    }

    @PostMapping("/oauth2/kakao/sdk")
    @Operation(summary = "Kakao SDK 로그인")
    public ResponseEntity<AuthResponse> kakaoSdkLogin(@Valid @RequestBody KakaoSdkLoginRequest request) {
        return oauthLogin(() -> oAuth2CodeAuthService.loginWithKakaoSdkAccessToken(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "토큰 갱신", description = "리프레시 토큰으로 액세스 토큰 재발급")
    public ResponseEntity<AuthResponse> refreshToken(@RequestHeader("X-Refresh-Token") String refreshToken) {
        return ResponseEntity.ok(authTokenService.refreshAccessToken(refreshToken));
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "리프레시 토큰 무효화")
    public ResponseEntity<MessageResponse> logout(@RequestHeader("Authorization") String accessToken) {
        authTokenService.logout(stripBearerPrefix(accessToken));
        return ResponseEntity.ok(MessageResponse.of("로그아웃이 완료되었습니다."));
    }

    private ResponseEntity<EmailResponse> toVerificationResponse(EmailVerificationResult result, String message) {
        return ResponseEntity.ok(EmailResponse.withVerification(
                result.getEmail(),
                message,
                result.getVerificationId(),
                result.getExpiresAt()
        ));
    }

    private ResponseEntity<AuthResponse> oauthLogin(Supplier<AuthResponse> loginSupplier) {
        return ResponseEntity.ok(loginSupplier.get());
    }

    private String stripBearerPrefix(String token) {
        return token.startsWith(BEARER_PREFIX) ? token.substring(BEARER_PREFIX.length()) : token;
    }
}
