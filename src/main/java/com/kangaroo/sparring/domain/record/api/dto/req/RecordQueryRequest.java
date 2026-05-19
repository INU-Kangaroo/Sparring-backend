package com.kangaroo.sparring.domain.record.api.dto.req;

import com.kangaroo.sparring.domain.record.common.RecordPeriodSupport;
import com.kangaroo.sparring.domain.record.common.type.RecordPeriod;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Clock;
import java.time.LocalDate;

@Getter
@Setter
public class RecordQueryRequest {

    @Parameter(description = "조회 기간 타입", example = "weekly")
    private String period = "daily";

    @Parameter(description = "기준 날짜 (daily/weekly/monthly에서 사용)", example = "2026-03-26")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date;

    @Parameter(description = "조회 연도 (period=monthly에서 선택)", example = "2026")
    private Integer year;

    @Parameter(description = "조회 월 (period=monthly에서 선택)", example = "3")
    private Integer month;

    @Parameter(description = "시작일 (period=range에서 필수)", example = "2026-03-01")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @Parameter(description = "종료일 (period=range에서 필수)", example = "2026-03-31")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    public RecordPeriodSupport.DateTimeRange toRange(Clock kstClock) {
        RecordPeriod resolvedPeriod = RecordPeriod.from(period);
        return RecordPeriodSupport.resolve(resolvedPeriod, date, year, month, startDate, endDate, kstClock);
    }
}
