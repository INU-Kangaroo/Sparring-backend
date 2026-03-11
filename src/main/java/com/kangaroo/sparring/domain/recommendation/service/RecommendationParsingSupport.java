package com.kangaroo.sparring.domain.recommendation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendationParsingSupport {

    private final ObjectMapper objectMapper;

    public JsonNode readTreeOrThrow(String json, String rawResponse, String context) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            log.error("{} 파싱 실패: body={}", context, RecommendationJsonSupport.abbreviate(rawResponse), e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
