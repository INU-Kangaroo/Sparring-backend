package com.kangaroo.sparring.domain.chatbot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kangaroo.sparring.domain.chatbot.session.ChatMessage;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class GeminiStreamingClient {

    private static final String SYSTEM_PROMPT = """
            당신은 건강 관리 전문 AI 어시스턴트입니다.
            혈압, 혈당, 운동, 영양, 수면 등 건강 관련 주제에 대해 친절하고 정확하게 답변합니다.
            의학적 진단이나 처방을 제공하지 않으며, 전문 의료진 상담을 권장합니다.
            한국어로 답변하며, 이해하기 쉬운 언어를 사용합니다.
            답변은 간결하고 명확하게, 필요시 bullet point를 사용합니다.
            위험한 증상이 언급되면 즉시 의료 기관 방문을 권고합니다.
            """;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String streamingUrl;

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
    }

    /**
     * Gemini streamGenerateContent 엔드포인트를 호출하여 텍스트 토큰 청크를 Flux로 반환한다.
     * SSE 형식(data: {...})으로 수신한 응답에서 텍스트를 추출한다.
     */
    public Flux<String> streamChat(List<ChatMessage> history) {
        String url = streamingUrl + "?key=" + apiKey + "&alt=sse";
        Map<String, Object> requestBody = buildRequestBody(history);

        return webClient.post()
                .uri(url)
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
                .bodyToFlux(String.class)
                .filter(line -> line.startsWith("data: "))
                .map(line -> line.substring(6).trim())
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

    private Map<String, Object> buildRequestBody(List<ChatMessage> history) {
        Map<String, Object> systemInstruction = Map.of(
                "parts", List.of(Map.of("text", SYSTEM_PROMPT))
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
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && !candidates.isEmpty()) {
                String text = candidates.get(0)
                        .path("content")
                        .path("parts")
                        .get(0)
                        .path("text")
                        .asText("");
                return text.isEmpty() ? Mono.empty() : Mono.just(text);
            }
            return Mono.empty();
        } catch (Exception e) {
            log.debug("텍스트 추출 불가 청크 (무시): {}", jsonData);
            return Mono.empty();
        }
    }
}
