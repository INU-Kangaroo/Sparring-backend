package com.kangaroo.sparring.domain.survey.repository;

import com.kangaroo.sparring.domain.survey.entity.Answer;
import com.kangaroo.sparring.domain.survey.entity.SurveyType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AnswerRepository extends JpaRepository<Answer, Long> {

    @Query("SELECT a FROM Answer a " +
            "WHERE a.user.id = :userId AND a.question.id = :questionId")
    Optional<Answer> findByUserIdAndQuestionId(@Param("userId") Long userId,
                                               @Param("questionId") Long questionId);

    @Query("SELECT a FROM Answer a " +
            "JOIN FETCH a.question q " +
            "WHERE a.user.id = :userId AND q.survey.surveyType = :surveyType")
    List<Answer> findByUserIdAndSurveyType(@Param("userId") Long userId,
                                           @Param("surveyType") SurveyType surveyType);

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END " +
            "FROM Answer a " +
            "JOIN a.question q " +
            "WHERE a.user.id = :userId AND q.survey.surveyType = :surveyType")
    boolean existsByUserIdAndSurveyType(@Param("userId") Long userId,
                                        @Param("surveyType") SurveyType surveyType);
}
