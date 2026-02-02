package com.kangaroo.sparring.domain.recommendation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "근력 운동 정보")
public class StrengthExerciseDto {

    @Schema(description = "운동명", example = "스쿼트")
    private String name;

    @Schema(description = "지속 시간", example = "10분")
    private String duration;

    @Schema(description = "빈도", example = "2회")
    private String frequency;

    @Schema(description = "운동별 주의사항", example = "[\"허리가 꺾이지 않게 자세를 유지하세요.\", \"통증이 있으면 즉시 중단하세요.\"]")
    private List<String> precautions;

    public static StrengthExerciseDto of(String name, String duration, String frequency, List<String> precautions) {
        return StrengthExerciseDto.builder()
                .name(name)
                .duration(duration)
                .frequency(frequency)
                .precautions(precautions)
                .build();
    }
}
