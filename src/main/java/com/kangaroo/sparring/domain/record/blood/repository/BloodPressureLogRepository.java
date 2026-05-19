package com.kangaroo.sparring.domain.record.blood.repository;

import com.kangaroo.sparring.domain.record.blood.entity.BloodPressureLog;
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

    @Query("SELECT function('month', bpl.measuredAt) AS month, " +
            "AVG(bpl.systolic) AS avgSystolic, MAX(bpl.systolic) AS maxSystolic, MIN(bpl.systolic) AS minSystolic, " +
            "AVG(bpl.diastolic) AS avgDiastolic, MAX(bpl.diastolic) AS maxDiastolic, MIN(bpl.diastolic) AS minDiastolic, " +
            "COUNT(bpl.id) AS count " +
            "FROM BloodPressureLog bpl " +
            "WHERE bpl.user.id = :userId " +
            "AND bpl.measuredAt BETWEEN :startDate AND :endDate " +
            "AND bpl.isDeleted = false " +
            "GROUP BY function('month', bpl.measuredAt)")
    List<MonthlyBloodPressureStats> findMonthlyStatsByUserId(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // 보고서용: userId + 기간 조회
    List<BloodPressureLog> findByUserIdAndMeasuredAtBetweenAndIsDeletedFalse(
            Long userId,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    );

    boolean existsByUserIdAndUpdatedAtAfter(Long userId, LocalDateTime updatedAt);

    interface MonthlyBloodPressureStats {
        Integer getMonth();
        Double getAvgSystolic();
        Integer getMaxSystolic();
        Integer getMinSystolic();
        Double getAvgDiastolic();
        Integer getMaxDiastolic();
        Integer getMinDiastolic();
        Long getCount();
    }
}
