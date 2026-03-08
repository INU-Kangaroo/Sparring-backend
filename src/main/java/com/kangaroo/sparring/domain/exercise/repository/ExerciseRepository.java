package com.kangaroo.sparring.domain.exercise.repository;

import com.kangaroo.sparring.domain.exercise.entity.Exercise;
import com.kangaroo.sparring.domain.exercise.type.ExerciseCategory;
import com.kangaroo.sparring.domain.exercise.type.ExerciseImpactLevel;
import com.kangaroo.sparring.domain.exercise.type.ExerciseIntensityLevel;
import com.kangaroo.sparring.domain.exercise.type.ExerciseLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ExerciseRepository extends JpaRepository<Exercise, Long> {

    // 정확히 일치
    Optional<Exercise> findByExerciseName(String exerciseName);

    // 부분 일치 (운동명 포함 검색)
    @Query("SELECT e FROM Exercise e WHERE e.exerciseName LIKE %:keyword%")
    List<Exercise> findByExerciseNameContaining(@Param("keyword") String keyword);

    // 태그 필터링 (추천용)
    List<Exercise> findByCategoryAndIntensityAndLocation(
            ExerciseCategory category,
            ExerciseIntensityLevel intensity,
            ExerciseLocation location
    );

    // 안전 필터 포함 (고충격 제외)
    List<Exercise> findByCategoryAndIntensityAndLocationAndImpactLevel(
            ExerciseCategory category,
            ExerciseIntensityLevel intensity,
            ExerciseLocation location,
            ExerciseImpactLevel impactLevel
    );

    // 강도만으로 필터링
    List<Exercise> findByIntensity(ExerciseIntensityLevel intensity);
}
