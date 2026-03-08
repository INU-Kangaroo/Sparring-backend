package com.kangaroo.sparring.domain.food.log.repository;

import com.kangaroo.sparring.domain.food.log.entity.FoodLog;
import com.kangaroo.sparring.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FoodLogRepository extends JpaRepository<FoodLog, Long> {

    List<FoodLog> findByUserAndEatenAtBetweenAndIsDeletedFalseOrderByEatenAtAsc(
            User user,
            LocalDateTime start,
            LocalDateTime end
    );

    Optional<FoodLog> findByIdAndIsDeletedFalse(Long id);
}
