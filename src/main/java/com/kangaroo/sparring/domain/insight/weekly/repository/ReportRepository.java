package com.kangaroo.sparring.domain.insight.weekly.repository;

import com.kangaroo.sparring.domain.insight.weekly.entity.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    // 해당 주 보고서 조회 (월요일 startDate 기준)
    @Query("SELECT r FROM Report r WHERE r.user.id = :userId AND r.startDate = :startDate AND r.isDeleted = false")
    Optional<Report> findByUserIdAndStartDate(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate
    );

    // 목록 조회 (월별 필터 + 페이지네이션)
    @Query("""
            SELECT r
            FROM Report r
            WHERE r.user.id = :userId
              AND r.isDeleted = false
              AND (:year IS NULL OR function('year', r.startDate) = :year)
              AND (:month IS NULL OR function('month', r.startDate) = :month)
            """)
    Page<Report> findPageByUserIdAndYearMonth(
            @Param("userId") Long userId,
            @Param("year") Integer year,
            @Param("month") Integer month,
            Pageable pageable
    );

    // 단건 조회 (권한 포함)
    @Query("SELECT r FROM Report r WHERE r.id = :reportId AND r.user.id = :userId AND r.isDeleted = false")
    Optional<Report> findByIdAndUserId(
            @Param("reportId") Long reportId,
            @Param("userId") Long userId
    );
}
