package com.kangaroo.sparring.global.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiApiClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final GeminiApiKeySelector keySelector;

    public String generateContent(String prompt) {
        long startedAt = System.currentTimeMillis();
        int promptLength = prompt == null ? 0 : prompt.length();
        String apiUrl = keySelector.getApiUrl();
        List<String> keys = keySelector.nextKeyOrder();
        log.info("Gemini API 호출 시작: endpoint={}, promptLength={}, keyPoolSize={}", apiUrl, promptLength, keys.size());

        for (int attempt = 0; attempt < keys.size(); attempt++) {
            String apiKey = keys.get(attempt);
            String url = apiUrl + "?key=" + apiKey;
            try {
                HttpEntity<Map<String, Object>> request = buildRequest(prompt);
                ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

                if (response.getStatusCode() == HttpStatus.OK) {
                    String text = extractTextFromResponse(response.getBody());
                    log.info("Gemini API 호출 성공: endpoint={}, elapsedMs={}, responseLength={}, attempt={}",
                            apiUrl, System.currentTimeMillis() - startedAt, text.length(), attempt + 1);
                    return text;
                }

                log.error("Gemini API 호출 실패: endpoint={}, status={}, elapsedMs={}, attempt={}",
                        apiUrl, response.getStatusCode(), System.currentTimeMillis() - startedAt, attempt + 1);
                throw new CustomException(ErrorCode.RECOMMENDATION_AI_CALL_FAILED);
            } catch (HttpClientErrorException.TooManyRequests e) {
                log.warn("Gemini API 호출 한도 초과, 다음 키로 재시도: endpoint={}, status={}, elapsedMs={}, attempt={}/{}, body={}",
                        apiUrl, e.getStatusCode(), System.currentTimeMillis() - startedAt, attempt + 1, keys.size(),
                        abbreviate(e.getResponseBodyAsString()));
            } catch (HttpStatusCodeException e) {
                if (e.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE && attempt + 1 < keys.size()) {
                    log.warn("Gemini API 일시 과부하(503), 다음 키로 재시도: endpoint={}, status={}, elapsedMs={}, attempt={}/{}, body={}",
                            apiUrl, e.getStatusCode(), System.currentTimeMillis() - startedAt, attempt + 1, keys.size(),
                            abbreviate(e.getResponseBodyAsString()));
                    continue;
                }
                log.error("Gemini API HTTP 오류: endpoint={}, status={}, elapsedMs={}, attempt={}, body={}",
                        apiUrl, e.getStatusCode(), System.currentTimeMillis() - startedAt, attempt + 1,
                        abbreviate(e.getResponseBodyAsString()));
                throw new CustomException(ErrorCode.RECOMMENDATION_AI_CALL_FAILED);
            } catch (ResourceAccessException e) {
                if (attempt + 1 < keys.size()) {
                    log.warn("Gemini API 네트워크/타임아웃 오류, 다음 키로 재시도: endpoint={}, elapsedMs={}, attempt={}/{}, message={}",
                            apiUrl, System.currentTimeMillis() - startedAt, attempt + 1, keys.size(), e.getMessage());
                    continue;
                }
                log.error("Gemini API 네트워크/타임아웃 오류: endpoint={}, elapsedMs={}, attempt={}, message={}",
                        apiUrl, System.currentTimeMillis() - startedAt, attempt + 1, e.getMessage());
                throw new CustomException(ErrorCode.RECOMMENDATION_AI_CALL_FAILED);
            } catch (CustomException e) {
                throw e;
            } catch (Exception e) {
                log.error("Gemini API 호출 중 오류: endpoint={}, elapsedMs={}, attempt={}",
                        apiUrl, System.currentTimeMillis() - startedAt, attempt + 1, e);
                throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
            }
        }

        throw new CustomException(ErrorCode.RECOMMENDATION_AI_RATE_LIMIT);
    }

    private HttpEntity<Map<String, Object>> buildRequest(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", List.of(
                Map.of("parts", List.of(
                        Map.of("text", prompt)
                ))
        ));

        return new HttpEntity<>(requestBody, headers);
    }

    private String extractTextFromResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode candidates = root.path("candidates");

            if (candidates.isArray() && candidates.size() > 0) {
                JsonNode firstCandidate = candidates.get(0);
                JsonNode content = firstCandidate.path("content");
                JsonNode parts = content.path("parts");

                if (parts.isArray() && parts.size() > 0) {
                    JsonNode firstPart = parts.get(0);
                    return firstPart.path("text").asText();
                }
            }

            throw new CustomException(ErrorCode.RECOMMENDATION_AI_CALL_FAILED);
        } catch (Exception e) {
            log.error("Gemini 응답 파싱 실패: ", e);
            throw new CustomException(ErrorCode.RECOMMENDATION_AI_CALL_FAILED);
        }
    }

    private String abbreviate(String value) {
        if (value == null) {
            return "";
        }
        return value.length() <= 500 ? value : value.substring(0, 500) + "...";
    }
}
