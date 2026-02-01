package com.kangaroo.sparring.domain.healthprofile.service;

import com.kangaroo.sparring.domain.healthprofile.dto.req.UpdateHealthProfileRequest;
import com.kangaroo.sparring.domain.healthprofile.dto.res.HealthProfileResponse;
import com.kangaroo.sparring.domain.healthprofile.entity.HealthProfile;
import com.kangaroo.sparring.domain.healthprofile.repository.HealthProfileRepository;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HealthProfileService {

    private final HealthProfileRepository healthProfileRepository;

    /**
     * 건강 프로필 조회
     */
    public HealthProfileResponse getHealthProfile(Long userId) {
        HealthProfile healthProfile = healthProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.HEALTH_PROFILE_NOT_FOUND));

        return HealthProfileResponse.from(healthProfile);
    }

    /**
     * 건강 프로필 업데이트 (부분 업데이트)
     */
    @Transactional
    public HealthProfileResponse updateHealthProfile(Long userId, UpdateHealthProfileRequest request) {
        HealthProfile healthProfile = healthProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.HEALTH_PROFILE_NOT_FOUND));

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
                request.getAllergies(),
                request.getHealthGoal()
        );

        HealthProfile updatedProfile = healthProfileRepository.save(healthProfile);
        return HealthProfileResponse.from(updatedProfile);
    }
}