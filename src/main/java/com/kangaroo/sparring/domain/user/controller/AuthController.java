package com.kangaroo.sparring.domain.user.controller;

import com.kangaroo.sparring.domain.user.dto.req.EmailRequest;
import com.kangaroo.sparring.domain.user.dto.req.LoginRequest;
import com.kangaroo.sparring.domain.user.dto.req.SignupRequest;
import com.kangaroo.sparring.domain.user.dto.req.VerifyCodeRequest;
import com.kangaroo.sparring.domain.user.dto.res.AuthResponse;
import com.kangaroo.sparring.domain.user.dto.res.EmailResponse;
import com.kangaroo.sparring.domain.user.service.UserService;
import com.kangaroo.sparring.global.email.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "인증", description = "회원가입 및 로그인 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final EmailService emailService;

    /**
     * 이메일 인증코드 발송
     */
    @PostMapping("/send-verification")
    @Operation(summary = "이메일 인증코드 발송", description = "회원가입을 위한 이메일 인증코드를 발송합니다")
    public ResponseEntity<EmailResponse> sendVerificationCode(@Valid @RequestBody EmailRequest request) {
        emailService.sendVerificationCode(request.getEmail());
        return ResponseEntity.ok(EmailResponse.of(request.getEmail(), "인증코드가 발송되었습니다."));
    }

    /**
     * 이메일 인증코드 재발송
     */
    @PostMapping("/resend-verification")
    @Operation(summary = "이메일 인증코드 재발송", description = "이메일 인증코드를 재발송합니다 (1분 쿨다운)")
    public ResponseEntity<EmailResponse> resendVerificationCode(@Valid @RequestBody EmailRequest request) {
        emailService.resendVerificationCode(request.getEmail());
        return ResponseEntity.ok(EmailResponse.of(request.getEmail(), "인증코드가 재발송되었습니다."));
    }

    /**
     * 인증코드 검증
     */
    @PostMapping("/verify-code")
    @Operation(summary = "인증코드 검증", description = "이메일로 받은 인증코드를 검증합니다")
    public ResponseEntity<EmailResponse> verifyCode(@Valid @RequestBody VerifyCodeRequest request) {
        emailService.verifyCode(request.getEmail(), request.getCode());
        return ResponseEntity.ok(EmailResponse.of(request.getEmail(), "이메일 인증이 완료되었습니다."));
    }

    /**
     * 회원가입
     */
    @Operation(summary = "회원가입", description = "이메일과 비밀번호로 회원가입합니다. 이메일 인증이 필요합니다.")
    @PostMapping("/signup")
    public ResponseEntity<EmailResponse> signup(@Valid @RequestBody SignupRequest request) {
        userService.signup(request);
        return ResponseEntity.ok(EmailResponse.of(request.getEmail(), "회원가입이 완료되었습니다."));
    }

    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인합니다")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = userService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 액세스 토큰 갱신
     */
    @PostMapping("/refresh")
    @Operation(summary = "토큰 갱신", description = "리프레시 토큰으로 새로운 액세스 토큰 발급")
    public ResponseEntity<AuthResponse> refreshToken(
            @RequestHeader("Authorization") String refreshToken) {
        
        // "Bearer " 제거
        if (refreshToken.startsWith("Bearer ")) {
            refreshToken = refreshToken.substring(7);
        }
        
        AuthResponse response = userService.refreshAccessToken(refreshToken);
        return ResponseEntity.ok(response);
    }

    /**
     * 로그아웃
     */
    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "리프레시 토큰 무효화")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String accessToken) {
        if (accessToken.startsWith("Bearer ")) {
            accessToken = accessToken.substring(7);
        }
        
        userService.logout(accessToken);
        return ResponseEntity.ok().build();
    }
}