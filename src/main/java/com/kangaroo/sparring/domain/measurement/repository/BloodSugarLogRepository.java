package com.kangaroo.sparring.domain.measurement.repository;

import com.kangaroo.sparring.domain.measurement.entity.BloodSugarLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
            @Param("limit") int limit
    );

    List<BloodSugarLog> findByUserIdAndIsDeletedFalseOrderByMeasurementTimeDesc(Long userId);

    @Query("SELECT b FROM BloodSugarLog b " +
            "WHERE b.user.id = :userId " +
            "AND YEAR(b.measurementTime) = :year " +
            "AND b.isDeleted = false " +
            "ORDER BY b.measurementTime ASC")
    List<BloodSugarLog> findByUserIdAndYear(
            @Param("userId") Long userId,
            @Param("year") int year
    );

    // 특정 월 조회
    @Query("SELECT b FROM BloodSugarLog b " +
            "WHERE b.user.id = :userId " +
            "AND YEAR(b.measurementTime) = :year " +
            "AND MONTH(b.measurementTime) = :month " +
            "AND b.isDeleted = false " +
            "ORDER BY b.measurementTime ASC")
    List<BloodSugarLog> findByUserIdAndYearAndMonth(
            @Param("userId") Long userId,
            @Param("year") int year,
            @Param("month") int month
    );
}