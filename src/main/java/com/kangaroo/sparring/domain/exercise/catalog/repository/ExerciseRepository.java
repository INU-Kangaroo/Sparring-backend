package com.kangaroo.sparring.domain.exercise.catalog.repository;

import com.kangaroo.sparring.domain.exercise.catalog.entity.Exercise;
import com.kangaroo.sparring.domain.exercise.catalog.type.ExerciseCategory;
import com.kangaroo.sparring.domain.exercise.catalog.type.ExerciseImpactLevel;
import com.kangaroo.sparring.domain.exercise.catalog.type.ExerciseLocation;
import com.kangaroo.sparring.domain.recommendation.type.ExerciseIntensity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ExerciseRepository extends JpaRepository<Exercise, Long> {

    Optional<Exercise> findByExerciseName(String exerciseName);

    @Query("SELECT e FROM Exercise e WHERE e.exerciseName LIKE %:keyword%")
    List<Exercise> findByExerciseNameContaining(@Param("keyword") String keyword);

    List<Exercise> findByCategoryAndIntensityAndLocation(
            ExerciseCategory category,
            ExerciseIntensity intensity,
            ExerciseLocation location
    );

    List<Exercise> findByCategoryAndIntensityAndLocationAndImpactLevel(
            ExerciseCategory category,
            ExerciseIntensity intensity,
            ExerciseLocation location,
            ExerciseImpactLevel impactLevel
    );

    List<Exercise> findByIntensity(ExerciseIntensity intensity);
}
