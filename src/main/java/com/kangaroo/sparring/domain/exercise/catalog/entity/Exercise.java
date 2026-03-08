package com.kangaroo.sparring.domain.exercise.catalog.entity;

import com.kangaroo.sparring.domain.common.BaseEntity;
import com.kangaroo.sparring.domain.exercise.catalog.type.ExerciseCategory;
import com.kangaroo.sparring.domain.exercise.catalog.type.ExerciseImpactLevel;
import com.kangaroo.sparring.domain.exercise.catalog.type.ExerciseLocation;
import com.kangaroo.sparring.domain.recommendation.type.ExerciseIntensity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "exercise",
        indexes = {
                @Index(name = "idx_exercise_category", columnList = "category"),
                @Index(name = "idx_exercise_intensity", columnList = "intensity"),
                @Index(name = "idx_exercise_location", columnList = "location")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Exercise extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exercise_name", nullable = false, length = 100)
    private String exerciseName;

    @Column(name = "met_value", nullable = false)
    private Double metValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExerciseCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ExerciseIntensity intensity;

    @Enumerated(EnumType.STRING)
    @Column(name = "impact_level", nullable = false, length = 20)
    private ExerciseImpactLevel impactLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ExerciseLocation location;
}
