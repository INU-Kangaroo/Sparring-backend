package com.kangaroo.sparring.domain.user.controller;

import com.kangaroo.sparring.domain.user.dto.req.ChangePasswordRequest;
import com.kangaroo.sparring.domain.user.dto.req.DeleteAccountRequest;
import com.kangaroo.sparring.domain.user.dto.req.UpdateUserProfileRequest;
import com.kangaroo.sparring.domain.user.dto.res.UserProfileResponse;
import com.kangaroo.sparring.domain.user.service.UserService;
import com.kangaroo.sparring.global.security.principal.UserIdPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "마이페이지", description = "프로필 조회/수정, 비밀번호 변경, 회원 탈퇴 API")
@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "프로필 조회", description = "현재 로그인 사용자의 프로필 조회")
    @GetMapping
    public ResponseEntity<UserProfileResponse> getProfile(
            @AuthenticationPrincipal UserIdPrincipal principal
    ) {
        Long userId = principal.getUserId();
        return ResponseEntity.ok(userService.getProfile(userId));
    }

    @Operation(summary = "프로필 수정", description = "이름/생년월일/키/몸무게 수정")
    @PatchMapping
    public ResponseEntity<UserProfileResponse> updateProfile(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @Valid @RequestBody UpdateUserProfileRequest request
    ) {
        Long userId = principal.getUserId();
        return ResponseEntity.ok(userService.updateProfile(userId, request));
    }

    @Operation(summary = "비밀번호 변경", description = "현재 비밀번호 확인 후 비밀번호 변경")
    @PatchMapping("/password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        Long userId = principal.getUserId();
        userService.changePassword(userId, request.getCurrentPassword(), request.getNewPassword(), request.getNewPasswordConfirm());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "회원 탈퇴", description = "계정 비활성화 처리")
    @DeleteMapping
    public ResponseEntity<Void> deleteAccount(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @RequestBody(required = false) DeleteAccountRequest request
    ) {
        Long userId = principal.getUserId();
        String password = request != null ? request.getPassword() : null;
        userService.deleteAccount(userId, password);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
