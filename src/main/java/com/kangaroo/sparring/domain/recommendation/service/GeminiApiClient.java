package com.kangaroo.sparring.domain.recommendation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    public String generateContent(String prompt) {
        try {
            String url = apiUrl + "?key=" + apiKey;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("contents", List.of(
                    Map.of("parts", List.of(
                            Map.of("text", prompt)
                    ))
            ));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                return extractTextFromResponse(response.getBody());
            }

            log.error("Gemini API 호출 실패: status={}", response.getStatusCode());
            throw new CustomException(ErrorCode.RECOMMENDATION_AI_CALL_FAILED);
        } catch (HttpClientErrorException.TooManyRequests e) {
            log.error("Gemini API 호출 한도 초과: status={}, body={}",
                    e.getStatusCode(), abbreviate(e.getResponseBodyAsString()));
            throw new CustomException(ErrorCode.RECOMMENDATION_AI_RATE_LIMIT);
        } catch (HttpStatusCodeException e) {
            log.error("Gemini API HTTP 오류: status={}, body={}",
                    e.getStatusCode(), abbreviate(e.getResponseBodyAsString()));
            throw new CustomException(ErrorCode.RECOMMENDATION_AI_CALL_FAILED);
        } catch (ResourceAccessException e) {
            log.error("Gemini API 네트워크/타임아웃 오류: {}", e.getMessage());
            throw new CustomException(ErrorCode.RECOMMENDATION_AI_CALL_FAILED);
        } catch (Exception e) {
            log.error("Gemini API 호출 중 오류: ", e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
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
