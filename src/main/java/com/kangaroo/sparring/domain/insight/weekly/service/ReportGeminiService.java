package com.kangaroo.sparring.domain.insight.weekly.service;

import com.kangaroo.sparring.domain.insight.weekly.dto.internal.ImprovementEvidence;
import com.kangaroo.sparring.domain.insight.weekly.dto.internal.ReportEvidence;
import com.kangaroo.sparring.domain.recommendation.service.GeminiApiClient;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportGeminiService {

    private final GeminiApiClient geminiApiClient;

    @Value("classpath:prompts/report/report_comment_prompt.txt")
    private Resource commentPromptResource;

    @Value("classpath:prompts/report/report_improvement_prompt.txt")
    private Resource improvementPromptResource;

    /**
     * AI 종합 코멘트 생성
     */
    public String generateComment(ReportEvidence evidence) {
        return generateComment(
                evidence.score().overallScore(),
                evidence.comment().bloodSugarAvg(),
                evidence.comment().systolicAvg(),
                evidence.comment().avgCaloriesPerDay(),
                evidence.comment().exerciseSessions()
        );
    }

    public String generateComment(
            int overallScore,
            Double bloodSugarAvg,       // null 가능
            Double systolicAvg,         // null 가능
            Double avgCaloriesPerDay,   // null 가능
            Integer exerciseSessions    // null 가능
    ) {
        try {
            String template = loadTemplate(commentPromptResource);
            String prompt = template
                    .replace("{overallScore}", String.valueOf(overallScore))
                    .replace("{bloodSugarAvg}", bloodSugarAvg != null ? String.format("%.1f", bloodSugarAvg) : "데이터 없음")
                    .replace("{systolicAvg}", systolicAvg != null ? String.format("%.0f", systolicAvg) : "데이터 없음")
                    .replace("{avgCaloriesPerDay}", avgCaloriesPerDay != null ? String.format("%.0f", avgCaloriesPerDay) : "데이터 없음")
                    .replace("{exerciseSessions}", exerciseSessions != null ? String.valueOf(exerciseSessions) : "데이터 없음");

            return geminiApiClient.generateContent(prompt).strip();
        } catch (CustomException e) {
            log.warn("보고서 AI 코멘트 생성 실패, fallback 사용: score={}", overallScore);
            return generateFallbackComment(overallScore);
        }
    }

    /**
     * 개선 방법 생성
     */
    public String generateImprovementTips(ImprovementEvidence improvement) {
        return generateImprovementTips(
                improvement.category().name(),
                improvement.timeLabel(),
                improvement.detail()
        );
    }

    public String generateImprovementTips(
            String category,
            String timeLabel,
            String detail
    ) {
        try {
            String template = loadTemplate(improvementPromptResource);
            String prompt = template
                    .replace("{category}", category)
                    .replace("{timeLabel}", timeLabel)
                    .replace("{detail}", detail);

            return geminiApiClient.generateContent(prompt).strip();
        } catch (CustomException e) {
            log.warn("보고서 개선 방법 생성 실패, fallback 사용: category={}", category);
            return generateFallbackTips(category);
        }
    }

    private String loadTemplate(Resource resource) {
        try {
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("프롬프트 템플릿 로드 실패: {}", resource.getFilename(), e);
            throw new CustomException(ErrorCode.REPORT_AI_CALL_FAILED);
        }
    }

    private String generateFallbackComment(int score) {
        if (score >= 80) return "이번 주 건강 관리를 정말 잘 하셨어요! 꾸준히 유지해보세요 💪";
        if (score >= 60) return "이번 주 전반적으로 잘 관리하고 계세요. 조금만 더 신경 쓰면 더 좋아질 거예요!";
        return "이번 주 건강 관리가 조금 아쉬웠어요. 다음 주엔 더 잘할 수 있을 거예요. 화이팅! 🌱";
    }

    private String generateFallbackTips(String category) {
        return switch (category) {
            case "BLOOD_SUGAR" -> "식사 후 가벼운 산책하기\n탄수화물 섭취량 줄이기\n규칙적인 식사 시간 지키기";
            case "BLOOD_PRESSURE" -> "나트륨 섭취 줄이기\n충분한 수분 섭취하기\n스트레스 관리하기";
            case "MEAL" -> "규칙적인 식사 시간 지키기\n끼니 거르지 않기\n과식 피하기";
            case "EXERCISE" -> "하루 30분 걷기\n엘리베이터 대신 계단 이용\n주말 야외 활동 계획 세우기";
            default -> "규칙적인 생활 습관 유지하기\n충분한 수면 취하기\n스트레스 관리하기";
        };
    }
}
