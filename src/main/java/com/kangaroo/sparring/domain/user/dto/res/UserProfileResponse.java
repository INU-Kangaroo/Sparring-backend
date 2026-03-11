package com.kangaroo.sparring.domain.user.dto.res;

import com.kangaroo.sparring.domain.healthprofile.entity.HealthProfile;
import com.kangaroo.sparring.domain.user.entity.User;
import com.kangaroo.sparring.domain.user.type.SocialProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@AllArgsConstructor
@Builder
@Schema(description = "마이페이지 프로필 응답")
public class UserProfileResponse {

    @Schema(description = "사용자 ID", example = "1")
    private Long userId;

    @Schema(description = "이름(닉네임)", example = "홍길동")
    private String username;

    @Schema(description = "이메일", example = "test@example.com")
    private String email;

    @Schema(description = "생년월일", example = "1995-03-12")
    private LocalDate birthDate;

    @Schema(description = "키(cm)", example = "175.5")
    private BigDecimal height;

    @Schema(description = "몸무게(kg)", example = "70.2")
    private BigDecimal weight;

    @Schema(description = "소셜 로그인 제공자", example = "KAKAO")
    private SocialProvider socialProvider;

    public static UserProfileResponse of(User user, HealthProfile profile) {
        LocalDate birthDate = profile != null ? profile.getBirthDate() : user.getBirthDate();
        BigDecimal height = profile != null ? profile.getHeight() : null;
        BigDecimal weight = profile != null ? profile.getWeight() : null;

        return UserProfileResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .birthDate(birthDate)
                .height(height)
                .weight(weight)
                .socialProvider(user.getProvider())
                .build();
    }
}
