package com.kangaroo.sparring.domain.measurement.repository;

import com.kangaroo.sparring.domain.measurement.entity.BloodSugarPrediction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BloodSugarPredictionRepository extends JpaRepository<BloodSugarPrediction, Long> {

    /**
     * 특정 사용자의 기간별 혈당 예측 결과 조회
     */
    @Query("SELECT p FROM BloodSugarPrediction p " +
            "WHERE p.user.id = :userId " +
            "AND p.predictionDate BETWEEN :startDate AND :endDate " +
            "AND p.isDeleted = false " +
            "ORDER BY p.predictionDate ASC")
    List<BloodSugarPrediction> findByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * 특정 사용자의 최근 혈당 예측 결과 조회
     */
    @Query("SELECT p FROM BloodSugarPrediction p " +
            "WHERE p.user.id = :userId " +
            "AND p.isDeleted = false " +
            "ORDER BY p.predictionDate DESC")
    List<BloodSugarPrediction> findRecentByUserId(
            @Param("userId") Long userId
    );

    /**
     * 특정 사용자의 특정 날짜 예측 결과 조회
     */
    @Query("SELECT p FROM BloodSugarPrediction p " +
            "WHERE p.user.id = :userId " +
            "AND p.predictionDate = :predictionDate " +
            "AND p.isDeleted = false")
    List<BloodSugarPrediction> findByUserIdAndPredictionDate(
            @Param("userId") Long userId,
            @Param("predictionDate") LocalDate predictionDate
    );
}
