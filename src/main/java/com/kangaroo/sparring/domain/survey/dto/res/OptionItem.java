package com.kangaroo.sparring.domain.survey.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "선택지 항목")
public class OptionItem {

    @Schema(description = "저장/전송 코드값", example = "NORMAL")
    private String code;

    @Schema(description = "화면 표시 라벨", example = "정상")
    private String label;
}
