package com.kangaroo.sparring.domain.insight.weekly.service;

import com.kangaroo.sparring.domain.insight.weekly.dto.internal.HighlightEvidence;
import com.kangaroo.sparring.domain.insight.weekly.dto.internal.ImprovementEvidence;
import com.kangaroo.sparring.domain.insight.weekly.dto.internal.ReportEvidence;
import com.kangaroo.sparring.global.client.GeminiApiClient;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportGeminiService {

    private final GeminiApiClient geminiApiClient;

    @Value("classpath:prompts/report/report_comment_prompt.txt")
    private Resource commentPromptResource;

    @Value("classpath:prompts/report/report_improvement_prompt.txt")
    private Resource improvementPromptResource;

    // ── AI 종합 코멘트 ──────────────────────────────────────────────────────────

    public String generateComment(ReportEvidence evidence) {
        long startedAt = System.currentTimeMillis();
        log.info("Gemini 주간 코멘트 생성 시작: score={}", evidence.score().overallScore());
        try {
            String template = loadTemplate(commentPromptResource);
            String prompt = template
                    .replace("{overallScore}", String.valueOf(evidence.score().overallScore()))
                    .replace("{bloodSugarAvg}", evidence.comment().bloodSugarAvg() != null
                            ? String.format("%.1f", evidence.comment().bloodSugarAvg()) : "데이터 없음")
                    .replace("{systolicAvg}", evidence.comment().systolicAvg() != null
                            ? String.format("%.0f", evidence.comment().systolicAvg()) : "데이터 없음")
                    .replace("{avgCaloriesPerDay}", evidence.comment().avgCaloriesPerDay() != null
                            ? String.format("%.0f", evidence.comment().avgCaloriesPerDay()) : "데이터 없음")
                    .replace("{exerciseSessions}", evidence.comment().exerciseSessions() != null
                            ? String.valueOf(evidence.comment().exerciseSessions()) : "데이터 없음")
                    .replace("{highlightsSummary}", summarizeHighlights(evidence.highlights()))
                    .replace("{improvementSummary}", summarizeImprovement(evidence.improvement()));

            String comment = geminiApiClient.generateContent(prompt).strip();
            log.info("Gemini 주간 코멘트 생성 성공: score={}, elapsedMs={}",
                    evidence.score().overallScore(), System.currentTimeMillis() - startedAt);
            return comment;
        } catch (CustomException e) {
            log.warn("보고서 AI 코멘트 생성 실패, fallback 사용: score={}", evidence.score().overallScore());
            return generateFallbackComment(evidence.score().overallScore());
        }
    }

    // ── 개선 방법 ───────────────────────────────────────────────────────────────

    public String generateImprovementTips(ImprovementEvidence improvement) {
        long startedAt = System.currentTimeMillis();
        log.info("Gemini 개선 방법 생성 시작: category={}", improvement.category());
        try {
            String template = loadTemplate(improvementPromptResource);
            String prompt = template
                    .replace("{category}", improvement.category().name())
                    .replace("{timeLabel}", improvement.timeLabel())
                    .replace("{detail}", improvement.detail())
                    .replace("{factsSummary}", summarizeFacts(improvement.facts()));

            String tips = geminiApiClient.generateContent(prompt).strip();
            log.info("Gemini 개선 방법 생성 성공: category={}, elapsedMs={}",
                    improvement.category(), System.currentTimeMillis() - startedAt);
            return tips;
        } catch (CustomException e) {
            log.warn("보고서 개선 방법 생성 실패, fallback 사용: category={}", improvement.category());
            return generateFallbackTips(improvement.category().name());
        }
    }

    // ── 내부 유틸 ───────────────────────────────────────────────────────────────

    private String loadTemplate(Resource resource) {
        try {
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("프롬프트 템플릿 로드 실패: {}", resource.getFilename(), e);
            throw new CustomException(ErrorCode.REPORT_AI_CALL_FAILED);
        }
    }

    private String summarizeHighlights(List<HighlightEvidence> highlights) {
        if (highlights == null || highlights.isEmpty()) return "없음";
        return highlights.stream()
                .limit(4)
                .map(h -> switch (h.type()) {
                    case GOOD    -> "✅ " + h.message();
                    case WARNING -> "⚠️ " + h.message();
                })
                .collect(Collectors.joining(" | "));
    }

    private String summarizeImprovement(ImprovementEvidence improvement) {
        if (improvement == null) return "없음";
        return improvement.category().name() + " / " + improvement.timeLabel() + " / " + improvement.detail();
    }

    private String summarizeFacts(Map<String, Object> facts) {
        if (facts == null || facts.isEmpty()) return "없음";
        return facts.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(", "));
    }

    // ── Fallback ────────────────────────────────────────────────────────────────

    private String generateFallbackComment(int score) {
        if (score >= 80) return "이번 주 건강 관리를 정말 잘 하셨어요! 꾸준히 유지해보세요 💪";
        if (score >= 60) return "이번 주 전반적으로 잘 관리하고 계세요. 조금만 더 신경 쓰면 더 좋아질 거예요!";
        return "이번 주 건강 관리가 조금 아쉬웠어요. 다음 주엔 더 잘할 수 있을 거예요. 화이팅! 🌱";
    }

    private String generateFallbackTips(String category) {
        return switch (category) {
            case "BLOOD_SUGAR"    -> "식사 후 가벼운 산책하기\n탄수화물 섭취량 줄이기\n규칙적인 식사 시간 지키기";
            case "BLOOD_PRESSURE" -> "나트륨 섭취 줄이기\n충분한 수분 섭취하기\n스트레스 관리하기";
            case "MEAL"           -> "규칙적인 식사 시간 지키기\n끼니 거르지 않기\n과식 피하기";
            case "EXERCISE"       -> "하루 30분 걷기\n엘리베이터 대신 계단 이용\n주말 야외 활동 계획 세우기";
            default               -> "규칙적인 생활 습관 유지하기\n충분한 수면 취하기\n스트레스 관리하기";
        };
    }
}
