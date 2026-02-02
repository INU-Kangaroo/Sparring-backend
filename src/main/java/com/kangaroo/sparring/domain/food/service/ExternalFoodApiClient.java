package com.kangaroo.sparring.domain.food.service;

import com.kangaroo.sparring.domain.food.entity.Food;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 외부 음식 API 클라이언트
 * 추후 연동 예정
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExternalFoodApiClient {

    // TODO: 식품의약품안전처 또는 농촌진흥청 API 연동
    // - API 키 설정
    // - HTTP 클라이언트 설정
    // - 검색 및 파싱 로직
    // - DB 캐싱 로직

    /**
     * 외부 API로 음식 검색 및 캐싱
     */
    public List<Food> searchAndCache(String keyword) {
        log.info("외부 API 호출 (미구현): keyword={}", keyword);
        // TODO: 구현 예정
        return Collections.emptyList();
    }
}
