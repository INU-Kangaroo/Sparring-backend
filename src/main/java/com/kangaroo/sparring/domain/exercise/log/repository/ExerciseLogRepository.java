package com.kangaroo.sparring.domain.exercise.log.repository;

import com.kangaroo.sparring.domain.exercise.log.entity.ExerciseLog;
import com.kangaroo.sparring.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ExerciseLogRepository extends JpaRepository<ExerciseLog, Long> {

    List<ExerciseLog> findByUserOrderByLoggedAtDesc(User user);

    List<ExerciseLog> findByUserAndLoggedAtBetweenOrderByLoggedAtDesc(
            User user,
            LocalDateTime from,
            LocalDateTime to
    );
}
