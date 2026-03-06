package com.kangaroo.sparring.domain.log.repository;

import com.kangaroo.sparring.domain.log.entity.MealLog;
import com.kangaroo.sparring.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MealLogRepository extends JpaRepository<MealLog, Long> {

    List<MealLog> findByUserAndEatenAtBetweenAndIsDeletedFalseOrderByEatenAtAsc(
            User user,
            LocalDateTime start,
            LocalDateTime end
    );

    Optional<MealLog> findByIdAndIsDeletedFalse(Long id);
}
