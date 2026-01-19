package com.kangaroo.sparring.domain.survey.repository;

import com.kangaroo.sparring.domain.survey.entity.Question;
import com.kangaroo.sparring.domain.survey.entity.SurveyType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findBySurveyIdOrderByQuestionOrderAsc(Long surveyId);

    @Query("SELECT q FROM Question q WHERE q.survey.surveyType = :surveyType ORDER BY q.questionOrder ASC")
    List<Question> findBySurveyType(@Param("surveyType") SurveyType surveyType);

    @Query("SELECT q FROM Question q WHERE q.id = :questionId AND q.survey.surveyType = :surveyType")
    Optional<Question> findByIdAndSurveyType(@Param("questionId") Long questionId,
                                             @Param("surveyType") SurveyType surveyType);

    @Query("SELECT q FROM Question q WHERE q.questionKey = :questionKey AND q.survey.surveyType = :surveyType")
    Optional<Question> findByQuestionKeyAndSurveyType(@Param("questionKey") String questionKey,
                                                      @Param("surveyType") SurveyType surveyType);

    Optional<Question> findByQuestionKey(String questionKey);
}
