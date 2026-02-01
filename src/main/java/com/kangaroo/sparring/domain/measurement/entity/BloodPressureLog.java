package com.kangaroo.sparring.domain.measurement.entity;

import com.kangaroo.sparring.domain.common.BaseEntity;
import com.kangaroo.sparring.domain.measurement.type.BloodPressureMeasurementType;
import com.kangaroo.sparring.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 혈압 측정 기록
 */
@Entity
@Table(name = "blood_pressure_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class BloodPressureLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "systolic", nullable = false)
    private Integer systolic;

    @Column(name = "diastolic", nullable = false)
    private Integer diastolic;

    @Column(name = "heart_rate")
    private Integer heartRate;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "measured_at", nullable = false)
    private LocalDateTime measuredAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "measurement_timing", nullable = false, length = 20)
    private BloodPressureMeasurementType measurementType;

    /**
     * 혈압 측정 기록 생성
     */
    public static BloodPressureLog create(User user, Integer systolic, Integer diastolic,
                                          Integer heartRate, LocalDateTime measuredAt,
                                          BloodPressureMeasurementType measurementType,
                                          String note) {
        return BloodPressureLog.builder()
                .user(user)
                .systolic(systolic)
                .diastolic(diastolic)
                .heartRate(heartRate)
                .note(note)
                .measuredAt(measuredAt)
                .measurementType(measurementType)
                .build();
    }
}
