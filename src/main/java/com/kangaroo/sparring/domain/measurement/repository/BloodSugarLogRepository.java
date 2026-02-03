package com.kangaroo.sparring.domain.measurement.repository;

import com.kangaroo.sparring.domain.measurement.entity.BloodSugarLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BloodSugarLogRepository extends JpaRepository<BloodSugarLog, Long> {

    @Query("SELECT b FROM BloodSugarLog b " +
            "WHERE b.user.id = :userId " +
            "AND b.measurementTime BETWEEN :startDate AND :endDate " +
            "AND b.isDeleted = false " +
            "ORDER BY b.measurementTime ASC")
    List<BloodSugarLog> findByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT b FROM BloodSugarLog b " +
            "WHERE b.user.id = :userId " +
            "AND b.isDeleted = false " +
            "ORDER BY b.measurementTime DESC")
    List<BloodSugarLog> findRecentByUserId(
            @Param("userId") Long userId,
            Pageable pageable
    );

    List<BloodSugarLog> findByUserIdAndIsDeletedFalseOrderByMeasurementTimeDesc(Long userId);

    List<BloodSugarLog> findByUserIdAndMeasurementTimeBetweenAndIsDeletedFalseOrderByMeasurementTimeAsc(
            Long userId,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    // 특정 기간 조회 (연/월 모두 공통 사용)

    @Query("SELECT function('month', b.measurementTime) AS month, " +
            "AVG(b.glucoseLevel) AS avgValue, MAX(b.glucoseLevel) AS maxValue, MIN(b.glucoseLevel) AS minValue, " +
            "COUNT(b.id) AS count " +
            "FROM BloodSugarLog b " +
            "WHERE b.user.id = :userId " +
            "AND b.measurementTime BETWEEN :startDate AND :endDate " +
            "AND b.isDeleted = false " +
            "GROUP BY function('month', b.measurementTime)")
    List<MonthlyBloodSugarStats> findMonthlyStatsByUserId(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    interface MonthlyBloodSugarStats {
        Integer getMonth();
        Double getAvgValue();
        Integer getMaxValue();
        Integer getMinValue();
        Long getCount();
    }
}
