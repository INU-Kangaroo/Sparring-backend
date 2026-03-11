package com.kangaroo.sparring.domain.survey.repository;

import com.kangaroo.sparring.domain.survey.entity.Survey;
import com.kangaroo.sparring.domain.survey.entity.SurveyType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SurveyRepository extends JpaRepository<Survey, Long> {

    Optional<Survey> findBySurveyType(SurveyType surveyType);

    boolean existsBySurveyType(SurveyType surveyType);
}
