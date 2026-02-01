package com.kangaroo.sparring.domain.measurement.repository;

import com.kangaroo.sparring.domain.measurement.entity.BloodPressureLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BloodPressureLogRepository extends JpaRepository<BloodPressureLog, Long> {

    /**
     * 사용자의 특정 기간 혈압 기록 조회
     */
    @Query("SELECT bpl FROM BloodPressureLog bpl " +
            "WHERE bpl.user.id = :userId " +
            "AND bpl.measuredAt BETWEEN :startDate AND :endDate " +
            "AND bpl.isDeleted = false " +
            "ORDER BY bpl.measuredAt ASC")
    List<BloodPressureLog> findByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * 사용자의 특정 기간 혈압 기록 조회 (월별)
     */
    List<BloodPressureLog> findByUserIdAndMeasuredAtBetweenAndIsDeletedFalseOrderByMeasuredAtAsc(
            Long userId,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    );

    /**
     * 사용자의 최근 N개 혈압 기록 조회
     */
    @Query("SELECT bpl FROM BloodPressureLog bpl " +
            "WHERE bpl.user.id = :userId " +
            "AND bpl.isDeleted = false " +
            "ORDER BY bpl.measuredAt DESC")
    List<BloodPressureLog> findRecentByUserId(
            @Param("userId") Long userId,
            Pageable pageable
    );
}
