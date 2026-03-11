package com.kangaroo.sparring.domain.food.catalog.controller;

import com.kangaroo.sparring.domain.food.catalog.dto.res.FoodDetailResponse;
import com.kangaroo.sparring.domain.food.catalog.dto.res.FoodResponse;
import com.kangaroo.sparring.domain.food.catalog.service.FoodService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "음식", description = "음식 영양 정보 API")
@RestController
@RequestMapping("/api/foods")
@RequiredArgsConstructor
public class FoodController {

    private final FoodService foodService;

    @Operation(
            summary = "음식 검색",
            description = "음식명으로 내부 DB를 검색한다."
    )
    @GetMapping("/search")
    public ResponseEntity<List<FoodResponse>> searchFood(
            @Parameter(description = "검색 키워드", example = "닭가슴살")
            @RequestParam String keyword,
            @Parameter(description = "페이지 크기 (1~50)", example = "20")
            @RequestParam(required = false) Integer size,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(required = false) Integer page
    ) {
        List<FoodResponse> foods = foodService.searchFood(keyword, size, page);
        return ResponseEntity.ok(foods);
    }

    @Operation(
            summary = "음식 상세 조회",
            description = "음식 ID로 상세 정보를 조회한다. 영양 정보가 포함된다."
    )
    @GetMapping("/{foodId}")
    public ResponseEntity<FoodDetailResponse> getFoodDetail(
            @Parameter(description = "음식 ID", example = "1")
            @PathVariable Long foodId
    ) {
        FoodDetailResponse foodDetail = foodService.getFoodDetail(foodId);
        return ResponseEntity.ok(foodDetail);
    }
}
