package com.kangaroo.sparring.domain.report.entity;

import com.kangaroo.sparring.domain.common.BaseEntity;
import com.kangaroo.sparring.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.List;

/**
 * 주간 종합 건강 보고서
 */
@Entity
@Table(
        name = "report",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_report_user_start_deleted", columnNames = {"user_id", "start_date", "is_deleted"})
        },
        indexes = {
                @Index(name = "idx_report_user_start", columnList = "user_id, start_date")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Report extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    // ── 헤더 통계 ──────────────────────────────────
    @Column(name = "record_days", nullable = false)
    private Integer recordDays;

    @Column(name = "blood_sugar_record_days", nullable = false)
    private Integer bloodSugarRecordDays;

    @Column(name = "blood_pressure_record_days", nullable = false)
    private Integer bloodPressureRecordDays;

    // ── 종합 점수 ──────────────────────────────────
    @Column(name = "overall_score", nullable = false)
    private Integer overallScore;

    @Column(name = "health_management_score", nullable = false)
    private Integer healthManagementScore;

    @Column(name = "measurement_consistency_score", nullable = false)
    private Integer measurementConsistencyScore;

    @Column(name = "lifestyle_score", nullable = false)
    private Integer lifestyleScore;

    // ── AI 코멘트 ──────────────────────────────────
    @Column(name = "ai_comment", columnDefinition = "TEXT")
    private String aiComment;

    // ── 개선 필요 (최하위 1개 영역) ──────────────────
    @Column(name = "improvement_category", length = 30)
    private String improvementCategory;   // BLOOD_SUGAR / BLOOD_PRESSURE / MEAL / EXERCISE / null

    @Column(name = "improvement_time_label", length = 100)
    private String improvementTimeLabel;  // "점심 식후 혈당", "운동 부족" 등

    @Column(name = "improvement_detail", length = 200)
    private String improvementDetail;     // "10번 중 7번 높음, 평균 165 (목표 140)"

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "improvement_tips", columnDefinition = "JSON")
    private List<String> improvementTips;

    public static Report create(
            User user,
            LocalDate startDate,
            LocalDate endDate,
            int recordDays,
            int bloodSugarRecordDays,
            int bloodPressureRecordDays,
            int overallScore,
            int healthManagementScore,
            int measurementConsistencyScore,
            int lifestyleScore,
            String aiComment,
            String improvementCategory,
            String improvementTimeLabel,
            String improvementDetail,
            List<String> improvementTips
    ) {
        return Report.builder()
                .user(user)
                .startDate(startDate)
                .endDate(endDate)
                .recordDays(recordDays)
                .bloodSugarRecordDays(bloodSugarRecordDays)
                .bloodPressureRecordDays(bloodPressureRecordDays)
                .overallScore(overallScore)
                .healthManagementScore(healthManagementScore)
                .measurementConsistencyScore(measurementConsistencyScore)
                .lifestyleScore(lifestyleScore)
                .aiComment(aiComment)
                .improvementCategory(improvementCategory)
                .improvementTimeLabel(improvementTimeLabel)
                .improvementDetail(improvementDetail)
                .improvementTips(improvementTips)
                .build();
    }
}
