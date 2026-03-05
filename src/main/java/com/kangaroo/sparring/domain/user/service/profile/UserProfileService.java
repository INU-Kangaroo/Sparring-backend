package com.kangaroo.sparring.domain.user.service.profile;

import com.kangaroo.sparring.domain.healthprofile.entity.HealthProfile;
import com.kangaroo.sparring.domain.healthprofile.repository.HealthProfileRepository;
import com.kangaroo.sparring.domain.user.dto.req.UpdateUserProfileRequest;
import com.kangaroo.sparring.domain.user.dto.res.UserProfileResponse;
import com.kangaroo.sparring.domain.user.entity.User;
import com.kangaroo.sparring.domain.user.repository.UserRepository;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserProfileService {

    private final UserRepository userRepository;
    private final HealthProfileRepository healthProfileRepository;

    public UserProfileResponse getProfile(Long userId) {
        User user = getUserOrThrow(userId);
        HealthProfile profile = healthProfileRepository.findByUserId(userId).orElse(null);
        return UserProfileResponse.of(user, profile);
    }

    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateUserProfileRequest request) {
        User user = getUserOrThrow(userId);
        HealthProfile profile = getOrCreateHealthProfile(user);

        if (request.getUsername() != null) {
            user.updateUsername(request.getUsername());
        }
        if (request.getBirthDate() != null) {
            user.updateBirthDate(request.getBirthDate());
        }

        profile.updateProfile(
                request.getBirthDate(),
                null,
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
        return UserProfileResponse.of(user, profile);
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    private HealthProfile getOrCreateHealthProfile(User user) {
        return healthProfileRepository.findByUserId(user.getId())
                .orElseGet(() -> healthProfileRepository.save(HealthProfile.builder().user(user).build()));
    }
}
