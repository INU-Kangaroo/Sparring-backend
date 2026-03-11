package com.kangaroo.sparring.domain.survey.repository;

import com.kangaroo.sparring.domain.survey.entity.Question;
import com.kangaroo.sparring.domain.survey.entity.QuestionStage;
import com.kangaroo.sparring.domain.survey.entity.SurveyType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findBySurveyIdOrderByQuestionOrderAsc(Long surveyId);

    @Query("SELECT q FROM Question q WHERE q.survey.surveyType = :surveyType AND q.questionStage = :questionStage ORDER BY q.questionOrder ASC")
    List<Question> findBySurveyTypeAndQuestionStage(@Param("surveyType") SurveyType surveyType,
                                                    @Param("questionStage") QuestionStage questionStage);

    @Query("SELECT q FROM Question q WHERE q.id = :questionId AND q.survey.surveyType = :surveyType AND q.questionStage = :questionStage")
    Optional<Question> findByIdAndSurveyTypeAndQuestionStage(@Param("questionId") Long questionId,
                                                             @Param("surveyType") SurveyType surveyType,
                                                             @Param("questionStage") QuestionStage questionStage);

    @Query("SELECT q FROM Question q WHERE q.questionKey = :questionKey AND q.survey.surveyType = :surveyType AND q.questionStage = :questionStage")
    Optional<Question> findByQuestionKeyAndSurveyTypeAndQuestionStage(@Param("questionKey") String questionKey,
                                                                      @Param("surveyType") SurveyType surveyType,
                                                                      @Param("questionStage") QuestionStage questionStage);

    Optional<Question> findByQuestionKey(String questionKey);
}
