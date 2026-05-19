package com.kangaroo.sparring.domain.record.steps.entity;

import com.kangaroo.sparring.domain.common.BaseEntity;
import com.kangaroo.sparring.domain.record.steps.type.StepSource;
import com.kangaroo.sparring.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "step_log",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_step_log_user_date_source_deleted",
                        columnNames = {"user_id", "step_date", "source", "is_deleted"}
                )
        },
        indexes = {
                @Index(name = "idx_step_log_user_step_date", columnList = "user_id, step_date")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StepLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "step_date", nullable = false)
    private LocalDate stepDate;

    @Column(name = "steps", nullable = false)
    private Integer steps;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    private StepSource source;

    @Column(name = "synced_at", nullable = false)
    private LocalDateTime syncedAt;

    @Builder
    private StepLog(User user, LocalDate stepDate, Integer steps, StepSource source, LocalDateTime syncedAt) {
        this.user = user;
        this.stepDate = stepDate;
        this.steps = steps;
        this.source = source;
        this.syncedAt = syncedAt;
    }

    public static StepLog create(User user, LocalDate stepDate, Integer steps, StepSource source, LocalDateTime syncedAt) {
        return StepLog.builder()
                .user(user)
                .stepDate(stepDate)
                .steps(steps)
                .source(source)
                .syncedAt(syncedAt)
                .build();
    }

    public void updateSteps(Integer steps, LocalDateTime syncedAt) {
        this.steps = steps;
        this.syncedAt = syncedAt;
    }
}
