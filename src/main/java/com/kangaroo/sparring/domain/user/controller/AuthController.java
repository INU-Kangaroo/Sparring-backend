package com.kangaroo.sparring.domain.user.controller;

import com.kangaroo.sparring.domain.user.dto.req.EmailRequest;
import com.kangaroo.sparring.domain.user.dto.req.GoogleSdkLoginRequest;
import com.kangaroo.sparring.domain.user.dto.req.KakaoSdkLoginRequest;
import com.kangaroo.sparring.domain.user.dto.req.LoginRequest;
import com.kangaroo.sparring.domain.user.dto.req.OAuth2PkceLoginRequest;
import com.kangaroo.sparring.domain.user.dto.req.SignupRequest;
import com.kangaroo.sparring.domain.user.dto.req.SocialSignupCompleteRequest;
import com.kangaroo.sparring.domain.user.dto.req.VerifyCodeRequest;
import com.kangaroo.sparring.domain.user.dto.res.AuthResponse;
import com.kangaroo.sparring.domain.user.dto.res.EmailResponse;
import com.kangaroo.sparring.domain.user.service.account.UserAccountService;
import com.kangaroo.sparring.domain.user.service.auth.AuthTokenService;
import com.kangaroo.sparring.domain.user.service.registration.UserRegistrationService;
import com.kangaroo.sparring.domain.user.service.auth.EmailService;
import com.kangaroo.sparring.domain.user.service.auth.EmailVerificationResult;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import com.kangaroo.sparring.global.response.MessageResponse;
import com.kangaroo.sparring.global.security.principal.PrincipalResolver;
import com.kangaroo.sparring.global.security.principal.UserIdPrincipal;
import com.kangaroo.sparring.global.security.oauth2.service.OAuth2CodeAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "인증", description = "회원가입 및 로그인 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRegistrationService userRegistrationService;
    private final UserAccountService userAccountService;
    private final AuthTokenService authTokenService;
    private final EmailService emailService;
    private final OAuth2CodeAuthService oAuth2CodeAuthService;

    /**
     * 이메일 인증코드 발송
     */
    @PostMapping("/send-verification")
    @Operation(
            summary = "이메일 인증코드 발송",
            description = "회원가입/이메일 변경용 인증코드 발송. verificationId 발급."
    )
    public ResponseEntity<EmailResponse> sendVerificationCode(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @Valid @RequestBody EmailRequest request
    ) {
        Long userId = principal != null ? principal.getUserId() : null;
        EmailVerificationResult result = emailService.sendVerificationCode(request.getEmail(), userId);
        return ResponseEntity.ok(EmailResponse.withVerification(
                result.getEmail(),
                "인증코드가 발송되었습니다.",
                result.getVerificationId(),
                result.getExpiresAt()
        ));
    }

    /**
     * 이메일 인증코드 재발송
     */
    @PostMapping("/resend-verification")
    @Operation(
            summary = "이메일 인증코드 재발송",
            description = "인증코드 재발송 (1분 쿨다운). 새 verificationId 발급."
    )
    public ResponseEntity<EmailResponse> resendVerificationCode(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @Valid @RequestBody EmailRequest request
    ) {
        Long userId = principal != null ? principal.getUserId() : null;
        EmailVerificationResult result = emailService.resendVerificationCode(request.getEmail(), userId);
        return ResponseEntity.ok(EmailResponse.withVerification(
                result.getEmail(),
                "인증코드가 재발송되었습니다.",
                result.getVerificationId(),
                result.getExpiresAt()
        ));
    }

    /**
     * 인증코드 검증
     */
    @PostMapping("/verify-code")
    @Operation(
            summary = "인증코드 검증",
            description = "verificationId + code로 인증코드 검증. 회원가입/이메일 변경 공용."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Created",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Success",
                                    value = """
                                            {
                                              "email": "test@example.com",
                                              "message": "이메일 인증이 완료되었습니다."
                                            }
                                            """
                            )
                    )
            )
    })
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

    /**
     * 회원가입
     */
    @Operation(summary = "회원가입", description = "이메일/비밀번호 회원가입 (이메일 인증 필요)")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Success",
                                    value = """
                                            {
                                              "message": "회원가입이 완료되었습니다."
                                            }
                                            """
                            )
                    )
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            name = "Signup",
                            value = """
                                    {
                                      "email": "test@example.com",
                                      "password": "password123!",
                                      "username": "홍길동",
                                      "birthDate": "1995-03-10",
                                      "gender": "MALE",
                                      "height": 175.5,
                                      "weight": 70.2,
                                      "bloodSugarStatus": "NORMAL",
                                      "bloodPressureStatus": "NORMAL",
                                      "medications": "없음",
                                      "allergies": "없음",
                                      "healthGoal": "혈당 관리",
                                      "hasFamilyHypertension": false
                                    }
                                    """
                    )
            )
    )
    @PostMapping("/signup")
    public ResponseEntity<MessageResponse> signup(@Valid @RequestBody SignupRequest request) {
        userRegistrationService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(MessageResponse.of("회원가입이 완료되었습니다."));
    }

    @Operation(summary = "로그인", description = "이메일/비밀번호 로그인")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authTokenService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 소셜 로그인 (Google/Kakao, SDK/PKCE 분리 엔드포인트)
     */
    @Operation(summary = "Google PKCE 로그인", description = "브라우저 기반 Google OAuth authorization code + PKCE로 JWT 발급")
    @PostMapping("/oauth2/google/pkce")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            name = "Google PKCE",
                            value = """
                                    {
                                      "authorizationCode": "4/0AfJohXk...",
                                      "redirectUri": "https://api.example.com/oauth/google/callback",
                                      "codeVerifier": "hJtX...sK9"
                                    }
                                    """
                    )
            )
    )
    public ResponseEntity<AuthResponse> googlePkceLogin(@Valid @RequestBody OAuth2PkceLoginRequest request) {
        return ResponseEntity.ok(oAuth2CodeAuthService.loginWithGooglePkce(request));
    }

    @Operation(summary = "Google SDK 로그인", description = "모바일 Google SDK가 발급한 server auth code로 JWT 발급")
    @PostMapping("/oauth2/google/sdk")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            name = "Google SDK",
                            value = """
                                    {
                                      "serverAuthCode": "4/0AfJohXk..."
                                    }
                                    """
                    )
            )
    )
    public ResponseEntity<AuthResponse> googleSdkLogin(@Valid @RequestBody GoogleSdkLoginRequest request) {
        return ResponseEntity.ok(oAuth2CodeAuthService.loginWithGoogleSdkCode(request));
    }

    @Operation(summary = "Kakao PKCE 로그인", description = "브라우저 기반 Kakao OAuth authorization code + PKCE로 JWT 발급 (redirectUri는 카카오 콘솔에 등록 가능한 URI 사용)")
    @PostMapping("/oauth2/kakao/pkce")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            name = "Kakao PKCE",
                            value = """
                                    {
                                      "authorizationCode": "abc123...",
                                      "redirectUri": "https://api.example.com/oauth/kakao/callback",
                                      "codeVerifier": "hJtX...sK9"
                                    }
                                    """
                    )
            )
    )
    public ResponseEntity<AuthResponse> kakaoPkceLogin(@Valid @RequestBody OAuth2PkceLoginRequest request) {
        return ResponseEntity.ok(oAuth2CodeAuthService.loginWithKakaoPkce(request));
    }

    @Operation(summary = "Kakao SDK 로그인", description = "모바일 Kakao SDK가 발급한 access token으로 JWT 발급")
    @PostMapping("/oauth2/kakao/sdk")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            name = "Kakao SDK",
                            value = """
                                    {
                                      "accessToken": "eyJhbGciOi..."
                                    }
                                    """
                    )
            )
    )
    public ResponseEntity<AuthResponse> kakaoSdkLogin(@Valid @RequestBody KakaoSdkLoginRequest request) {
        return ResponseEntity.ok(oAuth2CodeAuthService.loginWithKakaoSdkAccessToken(request));
    }

    @Operation(summary = "소셜 회원가입 완료", description = "소셜 로그인 후 기본 프로필 입력으로 회원가입 완료 처리")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Created",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Success",
                                    value = """
                                            {
                                              "message": "회원가입이 완료되었습니다."
                                            }
                                            """
                            )
                    )
            )
    })
    @PostMapping("/social/complete")
    public ResponseEntity<MessageResponse> completeSocialSignup(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @Valid @RequestBody SocialSignupCompleteRequest request
    ) {
        Long userId = PrincipalResolver.resolveUserId(principal);
        userRegistrationService.completeSocialSignup(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(MessageResponse.of("회원가입이 완료되었습니다."));
    }

    /**
     * 액세스 토큰 갱신
     */
    @PostMapping("/refresh")
    @Operation(summary = "토큰 갱신", description = "리프레시 토큰으로 액세스 토큰 재발급")
    public ResponseEntity<AuthResponse> refreshToken(
            @Parameter(description = "리프레시 토큰", required = true)
            @RequestHeader("X-Refresh-Token") String refreshToken) {

        AuthResponse response = authTokenService.refreshAccessToken(refreshToken);
        return ResponseEntity.ok(response);
    }

    /**
     * 로그아웃
     */
    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "리프레시 토큰 무효화")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Success",
                                    value = """
                                            {
                                              "message": "로그아웃이 완료되었습니다."
                                            }
                                            """
                            )
                    )
            )
    })
    public ResponseEntity<MessageResponse> logout(@RequestHeader("Authorization") String accessToken) {
        if (accessToken.startsWith("Bearer ")) {
            accessToken = accessToken.substring(7);
        }

        authTokenService.logout(accessToken);
        return ResponseEntity.ok(MessageResponse.of("로그아웃이 완료되었습니다."));
    }


}
