package com.kangaroo.sparring.domain.healthprofile.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kangaroo.sparring.domain.healthprofile.dto.req.UpdateHealthProfileRequest;
import com.kangaroo.sparring.domain.healthprofile.dto.res.HealthProfileResponse;
import com.kangaroo.sparring.domain.healthprofile.entity.HealthProfile;
import com.kangaroo.sparring.domain.healthprofile.repository.HealthProfileRepository;
import com.kangaroo.sparring.domain.user.entity.User;
import com.kangaroo.sparring.domain.user.repository.UserRepository;
import com.kangaroo.sparring.domain.user.service.UserLookupService;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HealthProfileService {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final HealthProfileRepository healthProfileRepository;
    private final UserRepository userRepository;
    private final UserLookupService userLookupService;

    /**
     * 건강 프로필 조회
     */
    public HealthProfileResponse getHealthProfile(Long userId) {
        long startedAt = System.currentTimeMillis();
        log.info("건강 프로필 조회 시작: userId={}", userId);
        HealthProfile healthProfile = getOrCreateHealthProfile(userId);
        HealthProfileResponse response = HealthProfileResponse.from(healthProfile);
        log.info("건강 프로필 조회 완료: userId={}, elapsedMs={}", userId, System.currentTimeMillis() - startedAt);
        return response;
    }

    /**
     * 건강 프로필 업데이트 (부분 업데이트)
     */
    @Transactional
    public HealthProfileResponse updateHealthProfile(Long userId, UpdateHealthProfileRequest request) {
        long startedAt = System.currentTimeMillis();
        log.info("건강 프로필 업데이트 시작: userId={}", userId);
        HealthProfile healthProfile = getOrCreateHealthProfile(userId);

        // 부분 업데이트
        healthProfile.updateProfile(
                request.getBirthDate(),
                request.getGender(),
                request.getHeight(),
                request.getWeight(),
                request.getBloodSugarStatus(),
                request.getBloodPressureStatus(),
                request.getHasFamilyHypertension(),
                request.getMedications(),
                toJsonArray(request.getAllergies()),
                request.getHealthGoal()
        );

        HealthProfile updatedProfile = healthProfileRepository.save(healthProfile);
        HealthProfileResponse response = HealthProfileResponse.from(updatedProfile);
        log.info("건강 프로필 업데이트 완료: userId={}, elapsedMs={}", userId, System.currentTimeMillis() - startedAt);
        return response;
    }

    @Transactional
    public HealthProfile getOrCreateHealthProfile(Long userId) {
        return healthProfileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userLookupService.getUserOrThrow(userId);
                    HealthProfile healthProfile = HealthProfile.builder()
                            .user(user)
                            .build();
                    log.info("건강 프로필 신규 생성: userId={}", userId);
                    return healthProfileRepository.save(healthProfile);
                });
    }

    private String toJsonArray(java.util.List<String> values) {
        if (values == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(values.stream()
                    .filter(v -> v != null && !v.isBlank())
                    .map(String::trim)
                    .toList());
        } catch (JsonProcessingException e) {
            log.warn("건강 프로필 배열 직렬화 실패, 빈 배열 대체");
            return "[]";
        }
    }
}
