package com.kangaroo.sparring.domain.user.controller;

import com.kangaroo.sparring.domain.user.dto.req.ChangePasswordRequest;
import com.kangaroo.sparring.domain.user.dto.req.DeleteAccountRequest;
import com.kangaroo.sparring.domain.user.dto.req.UpdateUserProfileRequest;
import com.kangaroo.sparring.domain.user.dto.res.UserDashboardResponse;
import com.kangaroo.sparring.domain.user.dto.res.UserHomeCardResponse;
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

    @Operation(summary = "마이페이지 대시보드 조회", description = "마이페이지 메인 화면용 요약 정보 조회")
    @GetMapping("/dashboard")
    public ResponseEntity<UserDashboardResponse> getDashboard(
            @AuthenticationPrincipal UserIdPrincipal principal
    ) {
        Long userId = PrincipalResolver.resolveUserId(principal);
        return ResponseEntity.ok(userProfileService.getDashboard(userId));
    }

    @Operation(summary = "메인 홈 카드 조회", description = "메인 화면 상단 프로필 카드용 정보 조회")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "FullData",
                                            value = """
                                                    {
                                                      "name": "홍길동",
                                                      "profileImageUrl": "https://cdn.example.com/profile/1.png",
                                                      "displayDate": "2026년 4월 8일 수요일",
                                                      "tags": ["23세", "여성", "제2형 당뇨", "고혈압 경계성"],
                                                      "tagCandidates": [
                                                        {"type": "AGE", "label": "23세"},
                                                        {"type": "GENDER", "label": "여성"},
                                                        {"type": "BLOOD_SUGAR", "label": "제2형 당뇨"},
                                                        {"type": "BLOOD_PRESSURE", "label": "고혈압 경계성"},
                                                        {"type": "EXERCISE", "label": "주 3~4회 운동"},
                                                        {"type": "SLEEP", "label": "평균 수면 6.5시간"},
                                                        {"type": "SMOKING", "label": "비흡연"},
                                                        {"type": "DRINKING", "label": "주 1~2회 음주"},
                                                        {"type": "MEDICATION", "label": "복용약 있음"},
                                                        {"type": "ALLERGY", "label": "알레르기 있음"}
                                                      ]
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "MinimalData",
                                            value = """
                                                    {
                                                      "name": "홍길동",
                                                      "profileImageUrl": null,
                                                      "displayDate": "2026년 4월 8일 수요일",
                                                      "tags": ["23세"],
                                                      "tagCandidates": [
                                                        {"type": "AGE", "label": "23세"}
                                                      ]
                                                    }
                                                    """
                                    )
                            }
                    )
            )
    })
    @GetMapping("/home-card")
    public ResponseEntity<UserHomeCardResponse> getHomeCard(
            @AuthenticationPrincipal UserIdPrincipal principal
    ) {
        Long userId = PrincipalResolver.resolveUserId(principal);
        return ResponseEntity.ok(userProfileService.getHomeCard(userId));
    }

    @Operation(summary = "프로필 수정", description = "이름/생년월일/성별/키/몸무게/프로필 이미지 수정")
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
