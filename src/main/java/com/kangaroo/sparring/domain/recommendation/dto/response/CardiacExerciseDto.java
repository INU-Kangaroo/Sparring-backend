package com.kangaroo.sparring.domain.recommendation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "유산소 운동 정보")
public class CardiacExerciseDto {

    @Schema(description = "운동명", example = "빨리걷기")
    private String name;

    @Schema(description = "지속 시간", example = "30분 이상")
    private String duration;

    @Schema(description = "예상 최소 칼로리 소모", example = "300")
    private Integer minCalories;

    @Schema(description = "예상 최대 칼로리 소모", example = "600")
    private Integer maxCalories;

    @Schema(description = "운동별 주의사항", example = "[\"무릎 통증이 있으면 강도를 낮추세요.\", \"운동 전후 스트레칭을 하세요.\"]")
    private List<String> precautions;

    public static CardiacExerciseDto of(String name, String duration, Integer minCalories, Integer maxCalories,
                                        List<String> precautions) {
        return CardiacExerciseDto.builder()
                .name(name)
                .duration(duration)
                .minCalories(minCalories)
                .maxCalories(maxCalories)
                .precautions(precautions)
                .build();
    }
}
