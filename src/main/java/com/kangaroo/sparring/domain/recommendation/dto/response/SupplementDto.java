package com.kangaroo.sparring.domain.recommendation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "영양제 정보")
public class SupplementDto {

    @Schema(description = "영양제명", example = "마그네슘")
    private String name;

    @Schema(description = "복용량", example = "1정")
    private String dosage;

    @Schema(description = "복용 빈도", example = "2회")
    private String frequency;

    @Schema(
            description = "효능 설명",
            example = "[\"인슐린 저항성을 낮춰 혈당 조절에 도움을 줄 수 있습니다.\", \"피로감을 줄이고 근육 기능 유지에 도움을 줄 수 있습니다.\"]"
    )
    private List<String> benefits;

    @Schema(
            description = "주의사항",
            example = "[\"복용 중인 약이 있다면 의사 또는 약사와 상의 후 섭취하세요.\", \"위장 불편감이 있으면 식후에 섭취하세요.\", \"권장 복용량을 초과하지 않도록 주의하세요.\"]"
    )
    private List<String> precautions;

    public static SupplementDto of(String name, String dosage, String frequency,
                                   List<String> benefits, List<String> precautions) {
        return SupplementDto.builder()
                .name(name)
                .dosage(dosage)
                .frequency(frequency)
                .benefits(benefits)
                .precautions(precautions)
                .build();
    }
}
