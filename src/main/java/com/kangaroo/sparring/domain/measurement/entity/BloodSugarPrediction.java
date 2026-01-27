package com.kangaroo.sparring.domain.measurement.entity;

import com.kangaroo.sparring.domain.common.BaseEntity;
import com.kangaroo.sparring.domain.measurement.type.RiskLevel;
import com.kangaroo.sparring.domain.measurement.type.TrendLabel;
import com.kangaroo.sparring.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 혈당 예측 결과
 */
@Entity
@Table(name = "blood_sugar_prediction")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class BloodSugarPrediction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "prediction_date", nullable = false)
    private LocalDate predictionDate;

    @Column(name = "predicted_value", nullable = false, precision = 5, scale = 2)
    private BigDecimal predictedValue;

    @Column(name = "confidence_score", precision = 5, scale = 2)
    private BigDecimal confidenceScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "trend_label", length = 20)
    private TrendLabel trendLabel;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", length = 20)
    private RiskLevel riskLevel;

    @Column(name = "is_data_insufficient", nullable = false)
    @Builder.Default
    private Boolean isDataInsufficient = false;

    /**
     * 혈당 예측 결과 생성
     */
    public static BloodSugarPrediction create(User user, LocalDate predictionDate,
                                              BigDecimal predictedValue,
                                              BigDecimal confidenceScore,
                                              TrendLabel trendLabel,
                                              RiskLevel riskLevel,
                                              Boolean isDataInsufficient) {
        return BloodSugarPrediction.builder()
                .user(user)
                .predictionDate(predictionDate)
                .predictedValue(predictedValue)
                .confidenceScore(confidenceScore)
                .trendLabel(trendLabel)
                .riskLevel(riskLevel)
                .isDataInsufficient(isDataInsufficient != null ? isDataInsufficient : false)
                .build();
    }
}