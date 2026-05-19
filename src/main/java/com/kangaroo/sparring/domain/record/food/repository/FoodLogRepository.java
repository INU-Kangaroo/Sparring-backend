package com.kangaroo.sparring.domain.record.food.repository;

import com.kangaroo.sparring.domain.common.type.MealTime;
import com.kangaroo.sparring.domain.record.food.entity.FoodLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface FoodLogRepository extends JpaRepository<FoodLog, Long> {

    List<FoodLog> findByUserIdAndEatenAtBetweenAndIsDeletedFalse(
            Long userId,
            LocalDateTime start,
            LocalDateTime end
    );

    List<FoodLog> findByUserIdAndEatenAtBetweenAndIsDeletedFalseOrderByEatenAtAsc(
            Long userId,
            LocalDateTime start,
            LocalDateTime end
    );

    Optional<FoodLog> findByIdAndIsDeletedFalse(Long id);

    boolean existsByUserIdAndMealTimeInAndUpdatedAtAfter(Long userId, Collection<MealTime> mealTimes, LocalDateTime updatedAt);
}
