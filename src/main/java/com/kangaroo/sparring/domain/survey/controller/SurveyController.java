package com.kangaroo.sparring.domain.survey.controller;

import com.kangaroo.sparring.domain.survey.dto.req.SurveySubmitRequest;
import com.kangaroo.sparring.domain.survey.dto.req.UpdateAnswerRequest;
import com.kangaroo.sparring.domain.survey.dto.res.AnswerResponse;
import com.kangaroo.sparring.domain.survey.dto.res.SurveyAnswersResponse;
import com.kangaroo.sparring.domain.survey.dto.res.SurveyQuestionsResponse;
import com.kangaroo.sparring.domain.survey.dto.res.SurveySubmitResponse;
import com.kangaroo.sparring.domain.survey.entity.SurveyType;
import com.kangaroo.sparring.domain.survey.service.SurveyService;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import com.kangaroo.sparring.global.security.principal.UserIdPrincipal;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "설문", description = "설문조사 API")
@RestController
@RequestMapping("/api/surveys")
@RequiredArgsConstructor
public class SurveyController {

    private final SurveyService surveyService;

    @Operation(summary = "설문 문항 조회", description = "설문 타입에 따른 문항 목록 조회")
    @GetMapping("/{surveyType}/questions")
    public ResponseEntity<SurveyQuestionsResponse> getSurveyQuestions(
            @PathVariable SurveyType surveyType
    ) {
        SurveyQuestionsResponse response = surveyService.getSurveyQuestions(surveyType);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "설문 응답 제출", description = "설문 응답 제출")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    examples = {
                            @ExampleObject(
                                    name = "BASIC",
                                    value = """
                                            {
                                              "surveyType": "BASIC",
                                              "answers": [
                                                { "questionKey": "BASIC_HEIGHT", "value": 175 },
                                                { "questionKey": "BASIC_GENDER", "value": "MALE" }
                                              ]
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "DETAILED",
                                    value = """
                                            {
                                              "surveyType": "DETAILED",
                                              "answers": [
                                                { "questionKey": "DETAILED_MEAL_FREQUENCY", "value": "ONE_TO_TWO" },
                                                { "questionKey": "DETAILED_FOOD_PREFERENCE", "value": ["CARB_HEAVY", "VEGETARIAN"] },
                                                { "questionKey": "DETAILED_EXERCISE_PLACE", "value": ["GYM_FACILITY", "OUTDOOR"] }
                                              ]
                                            }
                                            """
                            )
                    }
            )
    )
    @PostMapping
    public ResponseEntity<SurveySubmitResponse> submitSurvey(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @Valid @RequestBody SurveySubmitRequest request
    ) {
        Long userId = resolveUserId(principal);
        SurveySubmitResponse response = surveyService.submitSurvey(userId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "설문 응답 조회", description = "사용자가 작성한 설문 응답 조회")
    @GetMapping("/{surveyType}/answers")
    public ResponseEntity<SurveyAnswersResponse> getSurveyAnswers(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @PathVariable SurveyType surveyType
    ) {
        Long userId = resolveUserId(principal);
        SurveyAnswersResponse response = surveyService.getSurveyAnswers(userId, surveyType);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "설문 응답 수정", description = "특정 질문에 대한 응답 수정")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    examples = {
                            @ExampleObject(
                                    name = "Single Choice",
                                    value = """
                                            {
                                              "surveyType": "DETAILED",
                                              "questionKey": "DETAILED_MEAL_FREQUENCY",
                                              "value": "ONE_TO_TWO"
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "Multiple Choice",
                                    value = """
                                            {
                                              "surveyType": "DETAILED",
                                              "questionKey": "DETAILED_EXERCISE_PLACE",
                                              "value": ["GYM_FACILITY", "OUTDOOR"]
                                            }
                                            """
                            )
                    }
            )
    )
    @PatchMapping("/answers")
    public ResponseEntity<AnswerResponse> updateAnswer(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @Valid @RequestBody UpdateAnswerRequest request
    ) {
        Long userId = resolveUserId(principal);
        AnswerResponse response = surveyService.updateAnswer(userId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "설문 완료 여부 확인", description = "사용자가 해당 설문을 완료했는지 확인")
    @GetMapping("/{surveyType}/completed")
    public ResponseEntity<Boolean> checkSurveyCompleted(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @PathVariable SurveyType surveyType
    ) {
        Long userId = resolveUserId(principal);
        Boolean isCompleted = surveyService.checkSurveyCompleted(userId, surveyType);
        return ResponseEntity.ok(isCompleted);
    }

    private Long resolveUserId(UserIdPrincipal principal) {
        if (principal == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        return principal.getUserId();
    }
}
