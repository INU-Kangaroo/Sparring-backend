package com.kangaroo.sparring.domain.survey.service;

import com.kangaroo.sparring.domain.survey.dto.req.SurveySubmitRequest;
import com.kangaroo.sparring.domain.survey.dto.req.UpdateAnswerRequest;
import com.kangaroo.sparring.domain.survey.dto.res.AnswerResponse;
import com.kangaroo.sparring.domain.survey.dto.res.SurveyAnswersResponse;
import com.kangaroo.sparring.domain.survey.dto.res.SurveyQuestionsResponse;
import com.kangaroo.sparring.domain.survey.dto.res.SurveySubmitResponse;

import com.kangaroo.sparring.domain.healthprofile.entity.HealthProfile;
import com.kangaroo.sparring.domain.healthprofile.repository.HealthProfileRepository;
import com.kangaroo.sparring.domain.survey.entity.Answer;
import com.kangaroo.sparring.domain.survey.entity.Question;
import com.kangaroo.sparring.domain.survey.entity.Survey;
import com.kangaroo.sparring.domain.survey.entity.SurveyType;
import com.kangaroo.sparring.domain.survey.repository.AnswerRepository;
import com.kangaroo.sparring.domain.survey.repository.QuestionRepository;
import com.kangaroo.sparring.domain.survey.repository.SurveyRepository;
import com.kangaroo.sparring.domain.user.entity.User;
import com.kangaroo.sparring.domain.user.repository.UserRepository;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SurveyService {

    private final SurveyRepository surveyRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final HealthProfileRepository healthProfileRepository;
    private final UserRepository userRepository;

    /**
     * 설문 문항 조회
     */
    public SurveyQuestionsResponse getSurveyQuestions(SurveyType surveyType) {
        validateHealthProfileFieldMappings(surveyType);

        Survey survey = surveyRepository.findBySurveyType(surveyType)
                .orElseThrow(() -> new CustomException(ErrorCode.SURVEY_NOT_FOUND));

        return SurveyQuestionsResponse.from(survey);
    }

    /**
     * 설문 응답 제출
     */
    @Transactional
    public SurveySubmitResponse submitSurvey(Long userId, SurveySubmitRequest request) {
        // 이미 완료한 설문인지 확인
        if (answerRepository.existsByUserIdAndSurveyType(userId, request.getSurveyType())) {
            throw new CustomException(ErrorCode.SURVEY_ALREADY_COMPLETED);
        }

        validateHealthProfileFieldMappings(request.getSurveyType());

        // Survey 존재 여부 확인
        surveyRepository.findBySurveyType(request.getSurveyType())
                .orElseThrow(() -> new CustomException(ErrorCode.SURVEY_NOT_FOUND));

        Set<String> questionKeys = new HashSet<>();
        for (SurveySubmitRequest.AnswerItem answerItem : request.getAnswers()) {
            if (!questionKeys.add(answerItem.getQuestionKey())) {
                throw new CustomException(ErrorCode.INVALID_INPUT);
            }
        }

        List<Question> questions = questionRepository.findBySurveyType(request.getSurveyType());
        Set<String> requiredQuestionKeys = questions.stream()
                .map(Question::getQuestionKey)
                .collect(Collectors.toSet());

        if (questionKeys.size() != requiredQuestionKeys.size()
                || !requiredQuestionKeys.containsAll(questionKeys)) {
            throw new CustomException(ErrorCode.SURVEY_INCOMPLETE);
        }

        Map<String, Question> questionMap = questions.stream()
                .collect(Collectors.toMap(Question::getQuestionKey, question -> question));

        // User 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 답변 저장
        List<Answer> answers = request.getAnswers().stream()
                .map(answerItem -> {
                    Question question = questionMap.get(answerItem.getQuestionKey());
                    if (question == null) {
                        throw new CustomException(ErrorCode.QUESTION_NOT_FOUND);
                    }

                    return Answer.builder()
                            .user(user)
                            .question(question)
                            .answerText(answerItem.getAnswerText())
                            .build();
                })
                .collect(Collectors.toList());

        answerRepository.saveAll(answers);

        // HealthProfile 생성 또는 업데이트
        updateHealthProfile(userId, request.getSurveyType(), answers);

        return SurveySubmitResponse.of(request.getSurveyType());
    }

    /**
     * 설문 응답 수정
     */
    @Transactional
    public AnswerResponse updateAnswer(Long userId, UpdateAnswerRequest request) {
        Question question = questionRepository.findByQuestionKey(request.getQuestionKey())
                .orElseThrow(() -> new CustomException(ErrorCode.QUESTION_NOT_FOUND));

        Answer answer = answerRepository.findByUserIdAndQuestionId(userId, question.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.ANSWER_NOT_FOUND));

        answer.updateAnswer(request.getAnswerText());

        // HealthProfile 업데이트
        SurveyType surveyType = question.getSurvey().getSurveyType();
        updateHealthProfileFromSingleAnswer(userId, surveyType, answer);

        return AnswerResponse.from(answer);
    }

    private void validateHealthProfileFieldMappings(SurveyType surveyType) {
        List<Question> questions = questionRepository.findBySurveyType(surveyType);
        List<String> invalidFields = new ArrayList<>();
        for (Question question : questions) {
            String fieldName = question.getHealthProfileField();
            if (fieldName == null || fieldName.isBlank()) {
                continue;
            }
            if (!HealthProfile.isSupportedField(fieldName)) {
                invalidFields.add(question.getQuestionKey() + ":" + fieldName);
            }
        }
        if (!invalidFields.isEmpty()) {
            log.error("Invalid healthProfileField mappings: {}", String.join(", ", invalidFields));
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }

    /**
     * 설문 응답 조회
     */
    public SurveyAnswersResponse getSurveyAnswers(Long userId, SurveyType surveyType) {
        List<Answer> answers = answerRepository.findByUserIdAndSurveyType(userId, surveyType);

        if (answers.isEmpty()) {
            throw new CustomException(ErrorCode.SURVEY_NOT_COMPLETED);
        }

        List<AnswerResponse> answerResponses = answers.stream()
                .map(AnswerResponse::from)
                .collect(Collectors.toList());

        return SurveyAnswersResponse.of(surveyType, answerResponses);
    }

    /**
     * 설문 완료 여부 확인
     */
    public Boolean checkSurveyCompleted(Long userId, SurveyType surveyType) {
        return answerRepository.existsByUserIdAndSurveyType(userId, surveyType);
    }

    /**
     * HealthProfile 생성 또는 업데이트
     */
    private void updateHealthProfile(Long userId, SurveyType surveyType, List<Answer> answers) {
        HealthProfile healthProfile = healthProfileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
                    return HealthProfile.builder()
                            .user(user)
                            .build();
                });

        // 설문 타입에 따라 HealthProfile 업데이트
        if (surveyType == SurveyType.BASIC) {
            updateHealthProfileFromBasicSurvey(healthProfile, answers);
        } else if (surveyType == SurveyType.DETAILED) {
            updateHealthProfileFromDetailedSurvey(healthProfile, answers);
        }

        healthProfileRepository.save(healthProfile);
    }

    /**
     * 기본 설문 답변으로 HealthProfile 업데이트
     */
    private void updateHealthProfileFromBasicSurvey(HealthProfile healthProfile, List<Answer> answers) {
        applyHealthProfileUpdates(healthProfile, answers);
    }

    /**
     * 상세 설문 답변으로 HealthProfile 업데이트
     */
    private void updateHealthProfileFromDetailedSurvey(HealthProfile healthProfile, List<Answer> answers) {
        applyHealthProfileUpdates(healthProfile, answers);
    }

    /**
     * 단일 답변으로 HealthProfile 업데이트 (수정 시)
     */
    private void updateHealthProfileFromSingleAnswer(Long userId, SurveyType surveyType, Answer answer) {
        HealthProfile healthProfile = healthProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.HEALTH_PROFILE_NOT_FOUND));

        String fieldName = answer.getQuestion().getHealthProfileField();
        if (fieldName == null || fieldName.isBlank()) {
            return;
        }

        if (!HealthProfile.isSupportedField(fieldName)) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "지원하지 않는 healthProfileField: " + fieldName);
        }

        boolean applied = healthProfile.applySurveyField(fieldName, answer.getAnswerText());
        if (!applied) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "healthProfileField 값 형식 오류: " + fieldName);
        }
        healthProfileRepository.save(healthProfile);
    }

    private void applyHealthProfileUpdates(HealthProfile healthProfile, List<Answer> answers) {
        List<String> invalidFields = new ArrayList<>();
        for (Answer answer : answers) {
            String fieldName = answer.getQuestion().getHealthProfileField();
            if (fieldName == null || fieldName.isBlank()) {
                continue;
            }
            if (!HealthProfile.isSupportedField(fieldName)) {
                invalidFields.add(fieldName);
                continue;
            }
            boolean applied = healthProfile.applySurveyField(fieldName, answer.getAnswerText());
            if (!applied) {
                invalidFields.add(fieldName);
            }
        }

        if (!invalidFields.isEmpty()) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT,
                    "healthProfileField 검증 실패: " + String.join(", ", invalidFields)
            );
        }
    }
}
