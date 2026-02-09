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

    // 외부 API 연동 전까지는 빈 결과를 반환

    /**
     * 외부 API로 음식 검색 및 캐싱
     */
    public List<Food> searchAndCache(String keyword) {
        log.info("외부 API 미연동 상태: keyword={}", keyword);
        return Collections.emptyList();
    }
}
