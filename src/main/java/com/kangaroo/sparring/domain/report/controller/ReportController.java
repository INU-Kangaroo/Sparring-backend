package com.kangaroo.sparring.domain.report.controller;

import com.kangaroo.sparring.domain.report.dto.res.ReportListItemResponse;
import com.kangaroo.sparring.domain.report.dto.res.ReportHistoryPageResponse;
import com.kangaroo.sparring.domain.report.dto.res.ReportResponse;
import com.kangaroo.sparring.domain.report.service.ReportService;
import com.kangaroo.sparring.global.security.principal.PrincipalResolver;
import com.kangaroo.sparring.global.security.principal.UserIdPrincipal;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/insights/weekly")
@RequiredArgsConstructor
@Validated
@Tag(name = "보고서", description = "주간 종합 건강 보고서 API")
public class ReportController {

    private final ReportService reportService;

    @GetMapping
    @Operation(
            summary = "주간 보고서 조회",
            description = "date가 속한 주(월~일) 종합 건강 보고서를 반환. 없으면 생성 후 반환. date 미지정 시 오늘 기준"
    )
    public ResponseEntity<ReportResponse> getReportByDate(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @Parameter(description = "기준 날짜 (yyyy-MM-dd)", example = "2026-03-09")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Long userId = PrincipalResolver.resolveUserId(principal);
        return ResponseEntity.ok(reportService.getReportByDate(userId, date));
    }

    @GetMapping("/history")
    @Operation(
            summary = "지난 보고서 목록 조회",
            description = "월별 필터 + 페이지네이션으로 지난 보고서 목록을 반환"
    )
    public ResponseEntity<ReportHistoryPageResponse> getReportHistory(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @Parameter(description = "연도 필터", example = "2026")
            @RequestParam(required = false) Integer year,
            @Parameter(description = "월 필터 (1~12)", example = "3")
            @RequestParam(required = false) @Min(1) @Max(12) Integer month,
            @Parameter(description = "페이지 번호(0부터)", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) Integer page,
            @Parameter(description = "페이지 크기(1~100)", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size) {
        Long userId = PrincipalResolver.resolveUserId(principal);
        return ResponseEntity.ok(ReportHistoryPageResponse.from(
                reportService.getReportHistory(userId, year, month, page, size)
        ));
    }

    @GetMapping("/{reportId}")
    @Operation(
            summary = "특정 보고서 상세 조회",
            description = "reportId에 해당하는 보고서 상세를 반환"
    )
    public ResponseEntity<ReportResponse> getReport(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @PathVariable Long reportId) {
        Long userId = PrincipalResolver.resolveUserId(principal);
        return ResponseEntity.ok(reportService.getReport(userId, reportId));
    }
}
