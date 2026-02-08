package com.kangaroo.sparring.domain.recommendation.service;

import com.kangaroo.sparring.domain.healthprofile.entity.HealthProfile;
import com.kangaroo.sparring.domain.healthprofile.service.HealthProfileService;
import com.kangaroo.sparring.domain.measurement.entity.BloodPressureLog;
import com.kangaroo.sparring.domain.measurement.entity.BloodSugarLog;
import com.kangaroo.sparring.domain.measurement.repository.BloodPressureLogRepository;
import com.kangaroo.sparring.domain.measurement.repository.BloodSugarLogRepository;
import com.kangaroo.sparring.domain.user.entity.User;
import com.kangaroo.sparring.domain.user.repository.UserRepository;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecommendationContextService {

    private final UserRepository userRepository;
    private final HealthProfileService healthProfileService;
    private final BloodSugarLogRepository bloodSugarLogRepository;
    private final BloodPressureLogRepository bloodPressureLogRepository;

    public User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    public HealthProfile getOrCreateHealthProfile(Long userId) {
        return healthProfileService.getOrCreateHealthProfile(userId);
    }

    public List<BloodSugarLog> getRecentBloodSugars(Long userId, int count) {
        return bloodSugarLogRepository.findRecentByUserId(
                userId,
                PageRequest.of(0, count)
        );
    }

    public List<BloodPressureLog> getRecentBloodPressures(Long userId, int count) {
        return bloodPressureLogRepository.findRecentByUserId(
                userId,
                PageRequest.of(0, count)
        );
    }
}
