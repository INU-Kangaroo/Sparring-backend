package com.kangaroo.sparring.domain.user.service;

import com.kangaroo.sparring.domain.healthprofile.entity.HealthProfile;
import com.kangaroo.sparring.domain.healthprofile.repository.HealthProfileRepository;
import com.kangaroo.sparring.domain.record.common.BloodSugarRecord;
import com.kangaroo.sparring.domain.record.common.RecordReadService;
import com.kangaroo.sparring.domain.user.dto.req.UpdateUserProfileRequest;
import com.kangaroo.sparring.domain.user.dto.res.UserDashboardResponse;
import com.kangaroo.sparring.domain.user.dto.res.UserHomeCardResponse;
import com.kangaroo.sparring.domain.user.dto.res.UserProfileResponse;
import com.kangaroo.sparring.domain.user.entity.User;
import com.kangaroo.sparring.domain.user.repository.UserRepository;
import com.kangaroo.sparring.domain.user.type.Gender;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Locale;
import java.util.List;
import java.util.Set;
import java.util.LinkedHashSet;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserProfileService {

    private final UserRepository userRepository;
    private final UserLookupService userLookupService;
    private final HealthProfileRepository healthProfileRepository;
    private final RecordReadService recordReadService;
    private final Clock kstClock;

    public UserProfileResponse getProfile(Long userId) {
        long startedAt = System.currentTimeMillis();
        log.info("사용자 프로필 조회 시작: userId={}", userId);
        User user = userLookupService.getUserOrThrow(userId);
        HealthProfile profile = healthProfileRepository.findByUserId(userId).orElse(null);
        UserProfileResponse response = UserProfileResponse.of(user, profile);
        log.info("사용자 프로필 조회 완료: userId={}, elapsedMs={}", userId, System.currentTimeMillis() - startedAt);
        return response;
    }

    public UserDashboardResponse getDashboard(Long userId) {
        long startedAt = System.currentTimeMillis();
        log.info("사용자 대시보드 조회 시작: userId={}", userId);
        User user = userLookupService.getUserOrThrow(userId);
        HealthProfile profile = healthProfileRepository.findByUserId(userId).orElse(null);

        Long totalCount = recordReadService.countBloodSugarRecords(userId);
        BigDecimal average = roundOneDecimal(recordReadService.getAverageBloodSugar(userId));
        BigDecimal last7DaysAverage = getLast7DaysAverage(userId);
        int streak = calculateConsecutiveMeasurementDays(userId);

        LocalDate birthDate = profile != null ? profile.getBirthDate() : user.getBirthDate();
        var gender = profile != null && profile.getGender() != null ? profile.getGender() : user.getGender();
        BigDecimal height = profile != null ? profile.getHeight() : null;
        BigDecimal weight = profile != null ? profile.getWeight() : null;

        UserDashboardResponse response = UserDashboardResponse.builder()
                .profile(UserDashboardResponse.Profile.builder()
                        .username(user.getUsername())
                        .profileImageUrl(user.getProfileImageUrl())
                        .build())
                .record(UserDashboardResponse.Record.builder()
                        .totalMeasurementCount(totalCount != null ? totalCount : 0L)
                        .consecutiveMeasurementDays(streak)
                        .averageBloodSugarMgDl(average)
                        .last7DaysAverageBloodSugarMgDl(last7DaysAverage)
                        .build())
                .basicInfo(UserDashboardResponse.BasicInfo.builder()
                        .name(user.getUsername())
                        .birthDate(birthDate)
                        .gender(gender)
                        .height(height)
                        .weight(weight)
                        .build())
                .build();
        log.info("사용자 대시보드 조회 완료: userId={}, totalCount={}, streak={}, elapsedMs={}",
                userId, totalCount != null ? totalCount : 0L, streak, System.currentTimeMillis() - startedAt);
        return response;
    }

    public UserHomeCardResponse getHomeCard(Long userId) {
        long startedAt = System.currentTimeMillis();
        User user = userLookupService.getUserOrThrow(userId);
        HealthProfile profile = healthProfileRepository.findByUserId(userId).orElse(null);

        LocalDate birthDate = profile != null && profile.getBirthDate() != null ? profile.getBirthDate() : user.getBirthDate();
        Gender gender = profile != null && profile.getGender() != null ? profile.getGender() : user.getGender();
        List<UserHomeCardResponse.TagCandidate> tagCandidates = buildHomeTagCandidates(profile, birthDate, gender);

        UserHomeCardResponse response = UserHomeCardResponse.builder()
                .name(user.getUsername())
                .profileImageUrl(user.getProfileImageUrl())
                .displayDate(formatDisplayDate(LocalDate.now(kstClock)))
                .tags(tagCandidates.stream()
                        .limit(4)
                        .map(UserHomeCardResponse.TagCandidate::getLabel)
                        .toList())
                .tagCandidates(tagCandidates)
                .build();
        log.debug("홈 카드 조회 완료: userId={}, tagCandidates={}, elapsedMs={}",
                userId, tagCandidates.size(), System.currentTimeMillis() - startedAt);
        return response;
    }

    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateUserProfileRequest request) {
        long startedAt = System.currentTimeMillis();
        log.info("사용자 프로필 수정 시작: userId={}", userId);
        User user = userLookupService.getUserOrThrow(userId);
        HealthProfile profile = getOrCreateHealthProfile(user);

        if (request.getUsername() != null) {
            user.updateUsername(request.getUsername());
        }
        if (request.getBirthDate() != null) {
            user.updateBirthDate(request.getBirthDate());
        }
        if (request.getGender() != null) {
            user.updateGender(request.getGender());
        }
        if (request.getProfileImageUrl() != null) {
            user.updateProfileImageUrl(request.getProfileImageUrl());
        }

        profile.updateProfile(
                request.getBirthDate(),
                request.getGender(),
                request.getHeight(),
                request.getWeight(),
                null,
                null,
                null,
                null,
                null,
                null
        );

        healthProfileRepository.save(profile);
        UserProfileResponse response = UserProfileResponse.of(user, profile);
        log.info("사용자 프로필 수정 완료: userId={}, elapsedMs={}", userId, System.currentTimeMillis() - startedAt);
        return response;
    }

    private BigDecimal getLast7DaysAverage(Long userId) {
        LocalDate endDate = LocalDate.now(kstClock);
        LocalDate startDate = endDate.minusDays(6);
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        Double average = recordReadService.getAverageBloodSugar(
                userId, startDateTime, endDateTime
        );
        return roundOneDecimal(average);
    }

    private int calculateConsecutiveMeasurementDays(Long userId) {
        List<BloodSugarRecord> logs = recordReadService.getBloodSugarRecordsDesc(userId);
        Set<LocalDate> dateSet = new LinkedHashSet<>();
        for (BloodSugarRecord log : logs) {
            dateSet.add(log.getMeasurementTime().toLocalDate());
        }

        List<LocalDate> measuredDates = dateSet.stream().toList();
        if (measuredDates.isEmpty()) {
            return 0;
        }

        LocalDate expected = LocalDate.now(kstClock);
        int streak = 0;

        for (LocalDate measuredDate : measuredDates) {
            if (measuredDate == null) {
                continue;
            }
            if (!measuredDate.equals(expected)) {
                break;
            }
            streak++;
            expected = expected.minusDays(1);
        }

        return streak;
    }

    private String formatDisplayDate(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern("yyyy년 M월 d일 EEEE", Locale.KOREAN));
    }

    private List<UserHomeCardResponse.TagCandidate> buildHomeTagCandidates(
            HealthProfile profile,
            LocalDate birthDate,
            Gender gender
    ) {
        List<UserHomeCardResponse.TagCandidate> candidates = new ArrayList<>();
        addAgeTag(candidates, birthDate);
        addGenderTag(candidates, gender);

        if (profile != null) {
            if (profile.getBloodSugarStatus() != null) {
                String bloodSugarTag = switch (profile.getBloodSugarStatus()) {
                    case TYPE1, TYPE2 -> profile.getBloodSugarStatus().getDescription() + " 당뇨";
                    case BORDERLINE -> "당뇨 경계성";
                    case NORMAL -> "혈당 정상";
                    case UNKNOWN -> null;
                };
                addCandidate(candidates, "BLOOD_SUGAR", bloodSugarTag);
            }

            if (profile.getBloodPressureStatus() != null) {
                String bloodPressureTag = switch (profile.getBloodPressureStatus()) {
                    case BORDERLINE -> "고혈압 경계성";
                    case STAGE1, STAGE2 -> profile.getBloodPressureStatus().getDescription();
                    case NORMAL -> "혈압 정상";
                    case UNKNOWN -> null;
                };
                addCandidate(candidates, "BLOOD_PRESSURE", bloodPressureTag);
            }

            if (profile.getExerciseFrequency() != null) {
                String exerciseTag = switch (profile.getExerciseFrequency()) {
                    case DAILY -> "매일 운동";
                    default -> "주 " + profile.getExerciseFrequency().getDescription() + " 운동";
                };
                addCandidate(candidates, "EXERCISE", exerciseTag);
            }

            if (profile.getSleepHours() != null) {
                String sleepHours = profile.getSleepHours().stripTrailingZeros().toPlainString();
                addCandidate(candidates, "SLEEP", "평균 수면 " + sleepHours + "시간");
            }

            if (profile.getSmokingStatus() != null) {
                addCandidate(candidates, "SMOKING", profile.getSmokingStatus() ? "흡연" : "비흡연");
            }

            if (profile.getDrinkingFrequency() != null) {
                String drinkingTag = switch (profile.getDrinkingFrequency()) {
                    case NONE -> "음주 없음";
                    default -> profile.getDrinkingFrequency().getDescription() + " 음주";
                };
                addCandidate(candidates, "DRINKING", drinkingTag);
            }

            if (profile.getMedications() != null && !profile.getMedications().isBlank()) {
                addCandidate(candidates, "MEDICATION", "복용약 있음");
            }

            if (hasAllergyValue(profile.getAllergies())) {
                addCandidate(candidates, "ALLERGY", "알레르기 있음");
            }
        }
        return candidates;
    }

    private void addAgeTag(List<UserHomeCardResponse.TagCandidate> candidates, LocalDate birthDate) {
        LocalDate today = LocalDate.now(kstClock);
        if (birthDate == null || birthDate.isAfter(today)) {
            return;
        }
        int age = (int) ChronoUnit.YEARS.between(birthDate, today);
        if (age >= 0) {
            addCandidate(candidates, "AGE", age + "세");
        }
    }

    private void addGenderTag(List<UserHomeCardResponse.TagCandidate> candidates, Gender gender) {
        if (gender == null) {
            return;
        }
        String genderTag = switch (gender) {
            case MALE -> "남성";
            case FEMALE -> "여성";
            case OTHER -> "기타";
        };
        addCandidate(candidates, "GENDER", genderTag);
    }

    private void addCandidate(List<UserHomeCardResponse.TagCandidate> candidates, String type, String label) {
        if (label == null || label.isBlank()) {
            return;
        }
        candidates.add(UserHomeCardResponse.TagCandidate.builder()
                .type(type)
                .label(label)
                .build());
    }

    private boolean hasAllergyValue(String allergies) {
        if (allergies == null) {
            return false;
        }
        String trimmed = allergies.trim();
        return !trimmed.isEmpty() && !"[]".equals(trimmed) && !"null".equalsIgnoreCase(trimmed);
    }

    private BigDecimal roundOneDecimal(Double value) {
        if (value == null) {
            return null;
        }
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP);
    }

    private HealthProfile getOrCreateHealthProfile(User user) {
        return healthProfileRepository.findByUserId(user.getId())
                .orElseGet(() -> healthProfileRepository.save(HealthProfile.builder().user(user).build()));
    }
}
