package com.kangaroo.sparring.domain.measurement.entity;

import com.kangaroo.sparring.domain.common.BaseEntity;
import com.kangaroo.sparring.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 혈압 예측 데이터
 */
@Entity
@Table(name = "blood_pressure_prediction")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class BloodPressurePrediction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "predicted_at", nullable = false)
    private LocalDateTime predictedAt;

    @Column(name = "target_datetime", nullable = false)
    private LocalDateTime targetDatetime;

    @Column(name = "predicted_systolic", nullable = false)
    private Integer predictedSystolic;

    @Column(name = "predicted_diastolic", nullable = false)
    private Integer predictedDiastolic;

    /**
     * 혈압 예측 데이터 생성
     */
    public static BloodPressurePrediction create(User user, LocalDateTime predictedAt,
                                                 LocalDateTime targetDatetime,
                                                 Integer predictedSystolic,
                                                 Integer predictedDiastolic) {
        return BloodPressurePrediction.builder()
                .user(user)
                .predictedAt(predictedAt)
                .targetDatetime(targetDatetime)
                .predictedSystolic(predictedSystolic)
                .predictedDiastolic(predictedDiastolic)
                .build();
    }
}