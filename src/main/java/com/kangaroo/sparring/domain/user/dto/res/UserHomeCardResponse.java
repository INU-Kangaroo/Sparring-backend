package com.kangaroo.sparring.domain.user.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Builder
@Schema(description = "메인 홈 카드 응답")
public class UserHomeCardResponse {

    @Schema(description = "이름", example = "홍길동")
    private String name;

    @Schema(description = "프로필 이미지 URL", example = "https://cdn.example.com/profile/1.png", nullable = true)
    private String profileImageUrl;

    @Schema(description = "화면 표시 날짜", example = "2026년 4월 8일 수요일")
    private String displayDate;

    @Schema(description = "홈 카드 즉시 렌더링용 태그(상위 4개)", example = "[\"23세\", \"여성\", \"제2형 당뇨\"]")
    private List<String> tags;

    @Schema(description = "프론트 선택용 태그 후보 목록(우선순위 없음)")
    private List<TagCandidate> tagCandidates;

    @Getter
    @AllArgsConstructor
    @Builder
    @Schema(description = "태그 후보")
    public static class TagCandidate {
        @Schema(
                description = "태그 타입",
                example = "BLOOD_SUGAR",
                allowableValues = {"AGE", "GENDER", "BLOOD_SUGAR", "BLOOD_PRESSURE", "EXERCISE", "SLEEP", "SMOKING", "DRINKING", "MEDICATION", "ALLERGY"}
        )
        private String type;

        @Schema(description = "태그 라벨", example = "제2형 당뇨")
        private String label;
    }
}
