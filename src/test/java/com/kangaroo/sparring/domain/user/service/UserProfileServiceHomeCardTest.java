package com.kangaroo.sparring.domain.user.service;

import com.kangaroo.sparring.domain.healthprofile.entity.HealthProfile;
import com.kangaroo.sparring.domain.healthprofile.repository.HealthProfileRepository;
import com.kangaroo.sparring.domain.record.common.RecordReadService;
import com.kangaroo.sparring.domain.survey.type.BloodPressureStatus;
import com.kangaroo.sparring.domain.survey.type.BloodSugarStatus;
import com.kangaroo.sparring.domain.survey.type.DrinkingFrequency;
import com.kangaroo.sparring.domain.survey.type.ExerciseFrequency;
import com.kangaroo.sparring.domain.user.dto.res.UserHomeCardResponse;
import com.kangaroo.sparring.domain.user.entity.User;
import com.kangaroo.sparring.domain.user.repository.UserRepository;
import com.kangaroo.sparring.domain.user.service.UserLookupService;
import com.kangaroo.sparring.domain.user.type.Gender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceHomeCardTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserLookupService userLookupService;
    @Mock
    private HealthProfileRepository healthProfileRepository;
    @Mock
    private RecordReadService recordReadService;
    @Mock
    private Clock kstClock;
    @InjectMocks
    private UserProfileService userProfileService;

    @Test
    void 홈카드_응답은_프로필_우선값과_요약정보를_내린다() {
        Long userId = 1L;
        User user = User.builder()
                .id(userId)
                .username("홍길동")
                .birthDate(LocalDate.of(1998, 11, 3))
                .gender(Gender.MALE)
                .profileImageUrl("https://cdn.example.com/u1.png")
                .build();
        HealthProfile profile = HealthProfile.builder()
                .userId(userId)
                .user(user)
                .birthDate(LocalDate.of(2003, 3, 31))
                .gender(Gender.FEMALE)
                .bloodSugarStatus(BloodSugarStatus.TYPE2)
                .bloodPressureStatus(BloodPressureStatus.BORDERLINE)
                .exerciseFrequency(ExerciseFrequency.THREE_TO_FOUR)
                .sleepHours(new BigDecimal("6.5"))
                .smokingStatus(false)
                .drinkingFrequency(DrinkingFrequency.ONE_TO_TWO_PER_WEEK)
                .medications("메트포르민")
                .allergies("[\"견과류\"]")
                .build();

        when(userLookupService.getUserOrThrow(userId)).thenReturn(user);
        when(healthProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(kstClock.getZone()).thenReturn(ZoneId.of("Asia/Seoul"));
        when(kstClock.instant()).thenReturn(Instant.parse("2026-05-19T00:00:00Z"));

        UserHomeCardResponse response = userProfileService.getHomeCard(userId);

        assertThat(response.getName()).isEqualTo("홍길동");
        assertThat(response.getProfileImageUrl()).isEqualTo("https://cdn.example.com/u1.png");
        assertThat(response.getDisplayDate()).contains("년").contains("월").contains("일");
        assertThat(response.getTags()).hasSize(4);
        assertThat(response.getTags()).contains("여성", "제2형 당뇨", "고혈압 경계성");
        assertThat(response.getTags()).doesNotContain("주 3~4회 운동");
        assertThat(response.getTagCandidates()).isNotEmpty();
        assertThat(response.getTagCandidates().stream()
                .map(UserHomeCardResponse.TagCandidate::getType))
                .containsAll(Set.of(
                        "AGE",
                        "GENDER",
                        "BLOOD_SUGAR",
                        "BLOOD_PRESSURE",
                        "EXERCISE",
                        "SLEEP",
                        "SMOKING",
                        "DRINKING",
                        "MEDICATION",
                        "ALLERGY"
                ));
    }
}
