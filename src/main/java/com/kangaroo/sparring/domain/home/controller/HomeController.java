package com.kangaroo.sparring.domain.home.controller;

import com.kangaroo.sparring.domain.home.dto.res.MainHomeResponse;
import com.kangaroo.sparring.domain.home.service.HomeService;
import com.kangaroo.sparring.global.security.principal.PrincipalResolver;
import com.kangaroo.sparring.global.security.principal.UserIdPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
@Tag(name = "메인", description = "메인 화면 통합 API")
public class HomeController {

    private final HomeService homeService;

    @Operation(summary = "메인 화면 통합 조회", description = "프로필 카드/오늘의 한마디/혈당 그래프/기록 상태/걸음수 통합 조회")
    @GetMapping
    public ResponseEntity<MainHomeResponse> getMainHome(
            @AuthenticationPrincipal UserIdPrincipal principal
    ) {
        Long userId = PrincipalResolver.resolveUserId(principal);
        return ResponseEntity.ok(homeService.getMainHome(userId));
    }
}
