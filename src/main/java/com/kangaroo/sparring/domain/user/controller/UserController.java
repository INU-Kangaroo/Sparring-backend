package com.kangaroo.sparring.domain.user.controller;

import com.kangaroo.sparring.domain.user.dto.req.ChangePasswordRequest;
import com.kangaroo.sparring.domain.user.dto.req.DeleteAccountRequest;
import com.kangaroo.sparring.domain.user.dto.req.UpdateUserProfileRequest;
import com.kangaroo.sparring.domain.user.dto.res.UserProfileResponse;
import com.kangaroo.sparring.domain.user.service.UserAccountService;
import com.kangaroo.sparring.domain.user.service.UserProfileService;
import com.kangaroo.sparring.global.response.MessageResponse;
import com.kangaroo.sparring.global.security.principal.PrincipalResolver;
import com.kangaroo.sparring.global.security.principal.UserIdPrincipal;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "마이페이지", description = "프로필 조회/수정, 비밀번호 변경, 회원 탈퇴 API")
@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserProfileService userProfileService;
    private final UserAccountService userAccountService;

    @Operation(summary = "프로필 조회", description = "현재 로그인 사용자의 프로필 조회")
    @GetMapping
    public ResponseEntity<UserProfileResponse> getProfile(
            @AuthenticationPrincipal UserIdPrincipal principal
    ) {
        Long userId = PrincipalResolver.resolveUserId(principal);
        return ResponseEntity.ok(userProfileService.getProfile(userId));
    }

    @Operation(summary = "프로필 수정", description = "이름/생년월일/키/몸무게 수정")
    @PatchMapping
    public ResponseEntity<UserProfileResponse> updateProfile(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @Valid @RequestBody UpdateUserProfileRequest request
    ) {
        Long userId = PrincipalResolver.resolveUserId(principal);
        return ResponseEntity.ok(userProfileService.updateProfile(userId, request));
    }

    @Operation(summary = "비밀번호 변경", description = "현재 비밀번호 확인 후 비밀번호 변경")
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
                                              "message": "비밀번호가 변경되었습니다."
                                            }
                                            """
                            )
                    )
            )
    })
    @PatchMapping("/password")
    public ResponseEntity<MessageResponse> changePassword(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        Long userId = PrincipalResolver.resolveUserId(principal);
        userAccountService.changePassword(
                userId,
                request.getCurrentPassword(),
                request.getNewPassword(),
                request.getNewPasswordConfirm()
        );
        return ResponseEntity.ok(MessageResponse.of("비밀번호가 변경되었습니다."));
    }

    @Operation(summary = "회원 탈퇴", description = "계정 비활성화 처리")
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
                                              "message": "회원 탈퇴가 완료되었습니다."
                                            }
                                            """
                            )
                    )
            )
    })
    @DeleteMapping
    public ResponseEntity<MessageResponse> deleteAccount(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @RequestBody(required = false) DeleteAccountRequest request
    ) {
        Long userId = PrincipalResolver.resolveUserId(principal);
        String password = request != null ? request.getPassword() : null;
        userAccountService.deleteAccount(userId, password);
        return ResponseEntity.ok(MessageResponse.of("회원 탈퇴가 완료되었습니다."));
    }
}
