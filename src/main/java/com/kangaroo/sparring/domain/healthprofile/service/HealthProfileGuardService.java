package com.kangaroo.sparring.domain.healthprofile.service;

import com.kangaroo.sparring.domain.healthprofile.entity.HealthProfile;
import com.kangaroo.sparring.domain.healthprofile.repository.HealthProfileRepository;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HealthProfileGuardService {

    private final HealthProfileRepository healthProfileRepository;

    public void ensureProfileComplete(Long userId) {
        HealthProfile profile = healthProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.PROFILE_INCOMPLETE));

        if (!isComplete(profile)) {
            throw new CustomException(ErrorCode.PROFILE_INCOMPLETE);
        }
    }

    private boolean isComplete(HealthProfile profile) {
        return profile.getBirthDate() != null
                && profile.getGender() != null
                && profile.getHeight() != null
                && profile.getWeight() != null;
    }
}
