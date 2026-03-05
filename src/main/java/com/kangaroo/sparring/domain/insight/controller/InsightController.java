package com.kangaroo.sparring.domain.insight.controller;

import com.kangaroo.sparring.domain.insight.dto.res.TodayInsightResponse;
import com.kangaroo.sparring.domain.insight.service.InsightService;
import com.kangaroo.sparring.global.security.principal.PrincipalResolver;
import com.kangaroo.sparring.global.security.principal.UserIdPrincipal;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/insights")
@RequiredArgsConstructor
@Tag(name = "인사이트", description = "사용자 인사이트 API")
public class InsightController {

    private final InsightService insightService;

    @GetMapping("/today")
    @Operation(
            summary = "오늘의 인사이트 조회",
            description = "사용자의 최근 측정 데이터 기반으로 시간대 맞춤 인사이트를 반환합니다."
    )
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
                                              "type": "BOTH_STABLE",
                                              "message": "혈당과 혈압 모두 안정적으로 관리되고 있어요. 오늘도 좋은 리듬 유지해봐요 🙂"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<TodayInsightResponse> getTodayInsight(
            @AuthenticationPrincipal UserIdPrincipal principal) {
        Long userId = PrincipalResolver.resolveUserId(principal);
        return ResponseEntity.ok(insightService.getTodayInsight(userId));
    }
}
