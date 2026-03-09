package com.kangaroo.sparring.domain.insight.weekly.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Builder
@Schema(description = "보고서 히스토리 페이지 응답")
public class ReportHistoryPageResponse {

    @Schema(description = "목록 데이터")
    private List<ReportListItemResponse> items;

    @Schema(description = "현재 페이지 번호(0부터)", example = "0")
    private int page;

    @Schema(description = "페이지 크기", example = "20")
    private int size;

    @Schema(description = "전체 항목 수", example = "37")
    private long totalElements;

    @Schema(description = "전체 페이지 수", example = "2")
    private int totalPages;

    @Schema(description = "다음 페이지 존재 여부", example = "true")
    private boolean hasNext;

    @Schema(description = "이전 페이지 존재 여부", example = "false")
    private boolean hasPrevious;

    public static ReportHistoryPageResponse from(Page<ReportListItemResponse> pageResult) {
        return ReportHistoryPageResponse.builder()
                .items(pageResult.getContent())
                .page(pageResult.getNumber())
                .size(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .hasNext(pageResult.hasNext())
                .hasPrevious(pageResult.hasPrevious())
                .build();
    }
}
