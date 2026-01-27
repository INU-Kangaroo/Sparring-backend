package com.kangaroo.sparring.domain.measurement.entity;

import com.kangaroo.sparring.domain.common.BaseEntity;
import com.kangaroo.sparring.domain.measurement.type.MeasurementType;
import com.kangaroo.sparring.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 혈당 측정 기록
 */
@Entity
@Table(name = "blood_sugar_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class BloodSugarLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "glucose_level", nullable = false)
    private Integer glucoseLevel;

    @Column(name = "measurement_time", nullable = false)
    private LocalDateTime measurementTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "measurement_type", nullable = false, length = 20)
    private MeasurementType measurementType;

    @Column(name = "note", length = 500)
    private String note;

    /**
     * 혈당 측정 기록 생성
     */
    public static BloodSugarLog create(User user, Integer glucoseLevel,
                                       LocalDateTime measurementTime,
                                       MeasurementType measurementType,
                                       String note) {
        return BloodSugarLog.builder()
                .user(user)
                .glucoseLevel(glucoseLevel)
                .measurementTime(measurementTime)
                .measurementType(measurementType)
                .note(note)
                .build();
    }

    /**
     * 메모 수정
     */
    public void updateNote(String note) {
        this.note = note;
    }
}