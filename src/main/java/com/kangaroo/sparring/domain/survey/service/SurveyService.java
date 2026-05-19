package com.kangaroo.sparring.domain.survey.service;

import com.kangaroo.sparring.domain.survey.dto.req.SurveySubmitRequest;
import com.kangaroo.sparring.domain.survey.dto.req.UpdateAnswerRequest;
import com.kangaroo.sparring.domain.survey.dto.res.AnswerResponse;
import com.kangaroo.sparring.domain.survey.dto.res.QuestionResponse;
import com.kangaroo.sparring.domain.survey.dto.res.SurveyAnswersResponse;
import com.kangaroo.sparring.domain.survey.dto.res.SurveyQuestionsResponse;
import com.kangaroo.sparring.domain.survey.dto.res.SurveySubmitResponse;

import com.kangaroo.sparring.domain.healthprofile.entity.HealthProfile;
import com.kangaroo.sparring.domain.healthprofile.service.HealthProfileService;
import com.kangaroo.sparring.domain.healthprofile.HealthProfileFieldSupport;
import com.kangaroo.sparring.domain.healthprofile.repository.HealthProfileRepository;
import com.kangaroo.sparring.domain.survey.entity.*;
import com.kangaroo.sparring.domain.survey.repository.AnswerRepository;
import com.kangaroo.sparring.domain.survey.repository.QuestionRepository;
import com.kangaroo.sparring.domain.survey.repository.SurveyRepository;
import com.kangaroo.sparring.domain.user.entity.User;
import com.kangaroo.sparring.domain.user.repository.UserRepository;
import com.kangaroo.sparring.domain.user.service.UserLookupService;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
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
    private final UserLookupService userLookupService;
    private final SurveyAnswerValidator surveyAnswerValidator;
    private final HealthProfileService healthProfileService;

    /**
     * 설문 문항 조회
     */
    public SurveyQuestionsResponse getSurveyQuestions() {
        long startedAt = System.currentTimeMillis();
        log.info("설문 문항 조회 시작");
        surveyRepository.findBySurveyType(SurveyType.SURVEY)
                .orElseThrow(() -> new CustomException(ErrorCode.SURVEY_NOT_FOUND));
        List<Question> questions = questionRepository.findBySurveyType(SurveyType.SURVEY);
        surveyAnswerValidator.validateHealthProfileFieldMappings(questions);

        List<QuestionResponse> responses = questions.stream()
                .sorted((a, b) -> Integer.compare(
                        a.getQuestionOrder() == null ? Integer.MAX_VALUE : a.getQuestionOrder(),
                        b.getQuestionOrder() == null ? Integer.MAX_VALUE : b.getQuestionOrder()
                ))
                .map(QuestionResponse::from)
                .collect(Collectors.toList());

        SurveyQuestionsResponse response = SurveyQuestionsResponse.from(responses);
        log.info("설문 문항 조회 완료: questionCount={}, elapsedMs={}",
                responses.size(), System.currentTimeMillis() - startedAt);
        return response;
    }

    /**
     * 설문 응답 제출
     */
    @Transactional
    public SurveySubmitResponse submitSurvey(Long userId, SurveySubmitRequest request) {
        long startedAt = System.currentTimeMillis();
        int submittedAnswers = request.getAnswers() == null ? 0 : request.getAnswers().size();
        log.info("설문 제출 시작: userId={}, answers={}", userId, submittedAnswers);
        // 이미 완료한 설문인지 확인
        if (answerRepository.existsByUserIdAndSurveyType(userId, SurveyType.SURVEY)) {
            log.warn("설문 제출 차단: 이미 완료됨 userId={}", userId);
            throw new CustomException(ErrorCode.SURVEY_ALREADY_COMPLETED);
        }

        // Survey 존재 여부 확인
        surveyRepository.findBySurveyType(SurveyType.SURVEY)
                .orElseThrow(() -> new CustomException(ErrorCode.SURVEY_NOT_FOUND));

        List<Question> questions = questionRepository.findBySurveyType(SurveyType.SURVEY);
        surveyAnswerValidator.validateHealthProfileFieldMappings(questions);

        Map<String, Question> questionMap = toQuestionMap(questions);
        surveyAnswerValidator.validateSubmittedAnswers(request.getAnswers(), questionMap);

        // User 조회
        User user = userLookupService.getUserOrThrow(userId);

        // 답변 저장
        List<Answer> answers = request.getAnswers().stream()
                .map(answerItem -> {
                    Question question = questionMap.get(answerItem.getQuestionKey());
                    return Answer.builder()
                            .user(user)
                            .question(question)
                            .answerText(surveyAnswerValidator.toStoredAnswer(question, answerItem.getValue()))
                            .build();
                })
                .collect(Collectors.toList());

        try {
            answerRepository.saveAll(answers);
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(ErrorCode.SURVEY_ALREADY_COMPLETED);
        }

        // HealthProfile 생성 또는 업데이트
        updateHealthProfile(userId, SurveyType.SURVEY, answers);

        log.info("설문 제출 완료: userId={}, savedAnswers={}, elapsedMs={}",
                userId, answers.size(), System.currentTimeMillis() - startedAt);
        return SurveySubmitResponse.of();
    }

    /**
     * 설문 응답 수정
     */
    @Transactional
    public AnswerResponse updateAnswer(Long userId, UpdateAnswerRequest request) {
        long startedAt = System.currentTimeMillis();
        log.info("설문 답변 수정 시작: userId={}, questionKey={}", userId, request.getQuestionKey());
        Question question = questionRepository
                .findByQuestionKeyAndSurveyType(
                        request.getQuestionKey(),
                        SurveyType.SURVEY
                )
                .orElseThrow(() -> new CustomException(ErrorCode.QUESTION_NOT_FOUND));

        Answer answer = answerRepository.findByUserIdAndQuestionId(userId, question.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.ANSWER_NOT_FOUND));

        answer.updateAnswer(surveyAnswerValidator.toStoredAnswer(question, request.getValue()));

        // HealthProfile 업데이트
        updateHealthProfileFromSingleAnswer(userId, answer);

        AnswerResponse response = AnswerResponse.from(answer);
        log.info("설문 답변 수정 완료: userId={}, questionKey={}, elapsedMs={}",
                userId, request.getQuestionKey(), System.currentTimeMillis() - startedAt);
        return response;
    }

    /**
     * 설문 응답 조회
     */
    public SurveyAnswersResponse getSurveyAnswers(Long userId) {
        long startedAt = System.currentTimeMillis();
        log.info("설문 응답 조회 시작: userId={}", userId);
        List<Answer> answers = answerRepository.findByUserIdAndSurveyType(userId, SurveyType.SURVEY);

        if (answers.isEmpty()) {
            log.warn("설문 응답 조회 실패: 미완료 userId={}", userId);
            throw new CustomException(ErrorCode.SURVEY_NOT_COMPLETED);
        }

        List<AnswerResponse> answerResponses = answers.stream()
                .map(AnswerResponse::from)
                .collect(Collectors.toList());

        SurveyAnswersResponse response = SurveyAnswersResponse.of(answerResponses);
        log.info("설문 응답 조회 완료: userId={}, answers={}, elapsedMs={}",
                userId, answerResponses.size(), System.currentTimeMillis() - startedAt);
        return response;
    }

    /**
     * 설문 완료 여부 확인
     */
    public Boolean checkSurveyCompleted(Long userId) {
        boolean completed = answerRepository.existsByUserIdAndSurveyType(userId, SurveyType.SURVEY);
        log.debug("설문 완료 여부 조회: userId={}, completed={}", userId, completed);
        return completed;
    }

    /**
     * HealthProfile 생성 또는 업데이트
     */
    private void updateHealthProfile(Long userId, SurveyType surveyType, List<Answer> answers) {
        HealthProfile healthProfile = healthProfileService.getOrCreateHealthProfile(userId);

        // 설문 타입에 따라 HealthProfile 업데이트
        if (surveyType == SurveyType.SURVEY) {
            applyHealthProfileUpdates(healthProfile, answers);
        }

        try {
            healthProfileRepository.save(healthProfile);
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "건강 프로필 값 형식 또는 범위 오류");
        }
    }

    /**
     * 단일 답변으로 HealthProfile 업데이트 (수정 시)
     */
    private void updateHealthProfileFromSingleAnswer(Long userId, Answer answer) {
        HealthProfile healthProfile = healthProfileService.getOrCreateHealthProfile(userId);

        String fieldName = answer.getQuestion().getHealthProfileField();
        if (fieldName == null || fieldName.isBlank()) {
            return;
        }

        if (!HealthProfileFieldSupport.isSupportedField(fieldName)) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "지원하지 않는 healthProfileField: " + fieldName);
        }

        boolean applied = healthProfile.applySurveyField(fieldName, answer.getAnswerText());
        if (!applied) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "healthProfileField 값 형식 오류: " + fieldName);
        }
        try {
            healthProfileRepository.save(healthProfile);
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "건강 프로필 값 형식 또는 범위 오류");
        }
    }

    private void applyHealthProfileUpdates(HealthProfile healthProfile, List<Answer> answers) {
        List<String> invalidFields = new ArrayList<>();
        for (Answer answer : answers) {
            String fieldName = answer.getQuestion().getHealthProfileField();
            if (fieldName == null || fieldName.isBlank()) {
                continue;
            }
            if (!HealthProfileFieldSupport.isSupportedField(fieldName)) {
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

    private Map<String, Question> toQuestionMap(List<Question> questions) {
        try {
            return questions.stream()
                    .collect(Collectors.toMap(Question::getQuestionKey, question -> question));
        } catch (IllegalStateException e) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "중복 questionKey가 존재합니다.");
        }
    }

}
