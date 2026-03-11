package com.kangaroo.sparring.domain.chatbot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kangaroo.sparring.domain.chatbot.session.ChatMessage;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class GeminiStreamingClient {

    private static final String SYSTEM_PROMPT_PATH = "prompts/chatbot/system_prompt.txt";

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String streamingUrl;
    private final String baseSystemPrompt;

    public GeminiStreamingClient(
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper,
            @Value("${gemini.api.key}") String apiKey,
            @Value("${gemini.api.url}") String geminiApiUrl) {
        this.webClient = webClientBuilder
                .codecs(config -> config.defaultCodecs().maxInMemorySize(512 * 1024))
                .build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.streamingUrl = geminiApiUrl.replace("generateContent", "streamGenerateContent");
        this.baseSystemPrompt = readPromptTemplate(SYSTEM_PROMPT_PATH);
    }

    /**
     * Gemini streamGenerateContent 엔드포인트를 호출하여 텍스트 토큰 청크를 Flux로 반환한다.
     * SSE 형식(data: {...})으로 수신한 응답에서 텍스트를 추출한다.
     */
    public Flux<String> streamChat(List<ChatMessage> history, String userContextSummary) {
        String url = streamingUrl + "?key=" + apiKey + "&alt=sse";
        Map<String, Object> requestBody = buildRequestBody(history, userContextSummary);

        return webClient.post()
                .uri(url)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(
                        status -> status.value() == 429,
                        response -> Mono.error(new CustomException(ErrorCode.CHATBOT_AI_RATE_LIMIT))
                )
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(String.class)
                                .doOnNext(body -> log.error("Gemini 스트리밍 API 오류: status={}, body={}",
                                        response.statusCode(), body))
                                .then(Mono.error(new CustomException(ErrorCode.CHATBOT_AI_CALL_FAILED)))
                )
                .bodyToFlux(ServerSentEvent.class)
                .mapNotNull(sse -> toJsonString(sse.data()))
                .map(String::trim)
                .filter(data -> !data.isBlank() && !data.equals("[DONE]"))
                .flatMap(this::extractTextChunk)
                .onErrorMap(
                        ex -> !(ex instanceof CustomException),
                        ex -> {
                            log.error("Gemini 스트리밍 오류: {}", ex.getMessage(), ex);
                            return new CustomException(ErrorCode.CHATBOT_AI_CALL_FAILED);
                        }
                );
    }

    private String toJsonString(Object data) {
        if (data == null) {
            return null;
        }
        if (data instanceof String text) {
            return text;
        }
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            log.warn("Gemini SSE data 직렬화 실패: type={}", data.getClass().getName(), e);
            return null;
        }
    }

    private Map<String, Object> buildRequestBody(List<ChatMessage> history, String userContextSummary) {
        String systemPrompt = baseSystemPrompt;
        if (userContextSummary != null && !userContextSummary.isBlank()) {
            systemPrompt = baseSystemPrompt + "\n\n다음은 현재 사용자 관련 참고 정보입니다.\n"
                    + userContextSummary
                    + "\n위 정보를 바탕으로 개인화된 답변을 하되, 정보가 부족하면 단정하지 마세요.";
        }

        Map<String, Object> systemInstruction = Map.of(
                "parts", List.of(Map.of("text", systemPrompt))
        );

        List<Map<String, Object>> contents = history.stream()
                .map(msg -> Map.<String, Object>of(
                        "role", msg.getRole().name().toLowerCase(),
                        "parts", List.of(Map.of("text", msg.getContent()))
                ))
                .collect(Collectors.toList());

        return Map.of(
                "system_instruction", systemInstruction,
                "contents", contents,
                "generationConfig", Map.of(
                        "temperature", 0.7,
                        "maxOutputTokens", 2048
                )
        );
    }

    private Mono<String> extractTextChunk(String jsonData) {
        try {
            JsonNode root = objectMapper.readTree(jsonData);
            String text = extractTextFromRoot(root);
            if (!text.isBlank()) {
                return Mono.just(text);
            }

            log.info("Gemini 텍스트 청크 없음: {}", jsonData);
            return Mono.empty();
        } catch (Exception e) {
            log.info("Gemini 텍스트 추출 불가 청크: {}", jsonData);
            return Mono.empty();
        }
    }

    private String extractTextFromRoot(JsonNode root) {
        if (root == null || root.isMissingNode() || root.isNull()) {
            return "";
        }

        if (root.isArray()) {
            StringBuilder merged = new StringBuilder();
            for (JsonNode node : root) {
                appendExtractedText(node, merged);
            }
            return merged.toString();
        }

        StringBuilder merged = new StringBuilder();
        appendExtractedText(root, merged);
        return merged.toString();
    }

    private void appendExtractedText(JsonNode node, StringBuilder out) {
        JsonNode candidates = node.path("candidates");
        if (!candidates.isArray()) {
            return;
        }

        for (JsonNode candidate : candidates) {
            JsonNode parts = candidate.path("content").path("parts");
            if (!parts.isArray()) {
                continue;
            }
            for (JsonNode part : parts) {
                String text = part.path("text").asText("");
                if (!text.isBlank()) {
                    out.append(text);
                }
            }
        }
    }

    private String readPromptTemplate(String path) {
        try {
            ClassPathResource resource = new ClassPathResource(path);
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("프롬프트 템플릿을 로드할 수 없습니다: " + path, e);
        }
    }
}
