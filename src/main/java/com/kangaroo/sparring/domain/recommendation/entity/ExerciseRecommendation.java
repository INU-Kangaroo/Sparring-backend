package com.kangaroo.sparring.domain.recommendation.entity;

import com.kangaroo.sparring.domain.recommendation.type.ExerciseType;
import com.kangaroo.sparring.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "exercise_recommendation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExerciseRecommendation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommendation_id", nullable = false)
    private Recommendation recommendation;

    @Enumerated(EnumType.STRING)
    @Column(name = "exercise_type", nullable = false, length = 20)
    private ExerciseType exerciseType;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 50)
    private String duration;

    @Column(name = "min_calories")
    private Integer minCalories;

    @Column(name = "max_calories")
    private Integer maxCalories;

    @Column(length = 50)
    private String frequency;

    @Column(columnDefinition = "TEXT")
    private String precautions;

    @Builder
    private ExerciseRecommendation(Recommendation recommendation, ExerciseType exerciseType,
                                   String name, String duration, Integer minCalories, Integer maxCalories,
                                   String frequency, String precautions) {
        this.recommendation = recommendation;
        this.exerciseType = exerciseType;
        this.name = name;
        this.duration = duration;
        this.minCalories = minCalories;
        this.maxCalories = maxCalories;
        this.frequency = frequency;
        this.precautions = precautions;
    }

    public static ExerciseRecommendation of(Recommendation recommendation, ExerciseType exerciseType,
                                            String name, String duration, Integer minCalories, Integer maxCalories,
                                            String frequency, String precautions) {
        return ExerciseRecommendation.builder()
                .recommendation(recommendation)
                .exerciseType(exerciseType)
                .name(name)
                .duration(duration)
                .minCalories(minCalories)
                .maxCalories(maxCalories)
                .frequency(frequency)
                .precautions(precautions)
                .build();
    }
}
