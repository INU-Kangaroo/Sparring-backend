package com.kangaroo.sparring.domain.record.insulin.service;

import com.kangaroo.sparring.domain.record.blood.support.MeasurementValidationSupport;
import com.kangaroo.sparring.domain.record.insulin.dto.req.InsulinLogCreateRequest;
import com.kangaroo.sparring.domain.record.insulin.dto.res.InsulinLogResponse;
import com.kangaroo.sparring.domain.record.insulin.entity.InsulinLog;
import com.kangaroo.sparring.domain.record.insulin.repository.InsulinLogRepository;
import com.kangaroo.sparring.domain.user.entity.User;
import com.kangaroo.sparring.domain.user.repository.UserRepository;
import com.kangaroo.sparring.domain.user.service.UserLookupService;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class InsulinLogService {

    private final InsulinLogRepository insulinLogRepository;
    private final UserRepository userRepository;
    private final UserLookupService userLookupService;
    private final Clock kstClock;

    @Transactional
    public InsulinLogResponse createInsulinLog(Long userId, InsulinLogCreateRequest request) {
        log.info("인슐린 기록 등록 시작: userId={}, eventType={}", userId, request.getEventType());

        User user = userLookupService.getUserOrThrow(userId);
        LocalDateTime usedAt = LocalDateTime.of(request.getUsedDate(), request.getUsedTime());
        MeasurementValidationSupport.validateMeasurementTime(usedAt, kstClock);

        boolean tempBasalActive = Boolean.TRUE.equals(request.getTempBasalActive());
        BigDecimal tempBasalValue = request.getTempBasalValue() == null ? BigDecimal.ZERO : request.getTempBasalValue();

        InsulinLog insulinLog = InsulinLog.create(
                user,
                request.getEventType(),
                request.getDose(),
                usedAt,
                request.getInsulinType(),
                tempBasalActive,
                tempBasalValue
        );

        InsulinLog savedLog = insulinLogRepository.save(insulinLog);
        log.info("인슐린 기록 등록 완료: logId={}", savedLog.getId());
        return InsulinLogResponse.from(savedLog);
    }

    public List<InsulinLogResponse> getInsulinLogs(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("인슐린 기록 조회: userId={}, startDate={}, endDate={}", userId, startDate, endDate);
        MeasurementValidationSupport.validateDateRange(startDate, endDate);
        return insulinLogRepository.findByUserIdAndDateRange(userId, startDate, endDate).stream()
                .map(InsulinLogResponse::from)
                .toList();
    }
}
