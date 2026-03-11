package com.kangaroo.sparring.domain.measurement.repository;

import com.kangaroo.sparring.domain.measurement.entity.BloodPressurePrediction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BloodPressurePredictionRepository extends JpaRepository<BloodPressurePrediction, Long> {

    /**
     * 사용자의 특정 기간 혈압 예측 조회
     */
    List<BloodPressurePrediction> findByUserIdAndTargetDatetimeBetweenAndIsDeletedFalseOrderByTargetDatetimeAsc(
            Long userId,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    );
}