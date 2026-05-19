package com.kangaroo.sparring.domain.record.steps.repository;

import com.kangaroo.sparring.domain.record.steps.dto.res.StepDailyResponse;
import com.kangaroo.sparring.domain.record.steps.entity.StepLog;
import com.kangaroo.sparring.domain.record.steps.type.StepSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StepLogRepository extends JpaRepository<StepLog, Long> {

    Optional<StepLog> findByUserIdAndStepDateAndSourceAndIsDeletedFalse(
            Long userId,
            LocalDate stepDate,
            StepSource source
    );

    @Query("""
            select coalesce(sum(s.steps), 0)
            from StepLog s
            where s.user.id = :userId
              and s.stepDate = :stepDate
              and s.isDeleted = false
            """)
    Integer sumStepsByUserIdAndStepDate(@Param("userId") Long userId, @Param("stepDate") LocalDate stepDate);

    @Query("""
            select max(s.syncedAt)
            from StepLog s
            where s.user.id = :userId
              and s.stepDate = :stepDate
              and s.isDeleted = false
            """)
    LocalDateTime findLatestSyncedAtByUserIdAndStepDate(@Param("userId") Long userId, @Param("stepDate") LocalDate stepDate);

    @Query("""
            select new com.kangaroo.sparring.domain.record.steps.dto.res.StepDailyResponse(
                s.stepDate,
                coalesce(sum(s.steps), 0)
            )
            from StepLog s
            where s.user.id = :userId
              and s.stepDate between :startDate and :endDate
              and s.isDeleted = false
            group by s.stepDate
            order by s.stepDate asc
            """)
    List<StepDailyResponse> findDailyStepsByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
