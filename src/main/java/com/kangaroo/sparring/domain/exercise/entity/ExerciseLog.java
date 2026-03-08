package com.kangaroo.sparring.domain.exercise.entity;

import com.kangaroo.sparring.domain.common.BaseEntity;
import com.kangaroo.sparring.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "exercise_log",
        indexes = {
                @Index(name = "idx_exercise_log_user_logged_at", columnList = "user_id, logged_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExerciseLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "exercise_name", nullable = false, length = 100)
    private String exerciseName;        // 사용자 입력 원본

    @Column(name = "matched_exercise_name", length = 100)
    private String matchedExerciseName; // 매핑된 마스터 운동명 (매핑 실패 시 null)

    @Column(name = "met_value", nullable = false)
    private Double metValue;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Column(name = "calories_burned", nullable = false)
    private Double caloriesBurned;

    @Column(name = "logged_at", nullable = false)
    private LocalDateTime loggedAt;

    @Builder
    private ExerciseLog(User user, String exerciseName, String matchedExerciseName,
                        Double metValue, Integer durationMinutes, Double caloriesBurned,
                        LocalDateTime loggedAt) {
        this.user = user;
        this.exerciseName = exerciseName;
        this.matchedExerciseName = matchedExerciseName;
        this.metValue = metValue;
        this.durationMinutes = durationMinutes;
        this.caloriesBurned = caloriesBurned;
        this.loggedAt = loggedAt;
    }

    public static ExerciseLog of(User user, String exerciseName, String matchedExerciseName,
                                 Double metValue, Integer durationMinutes, Double caloriesBurned,
                                 LocalDateTime loggedAt) {
        return ExerciseLog.builder()
                .user(user)
                .exerciseName(exerciseName)
                .matchedExerciseName(matchedExerciseName)
                .metValue(metValue)
                .durationMinutes(durationMinutes)
                .caloriesBurned(caloriesBurned)
                .loggedAt(loggedAt)
                .build();
    }
}
