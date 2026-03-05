package com.kangaroo.sparring.domain.user.service.registration;

import com.kangaroo.sparring.domain.healthprofile.entity.HealthProfile;
import com.kangaroo.sparring.domain.healthprofile.repository.HealthProfileRepository;
import com.kangaroo.sparring.domain.user.dto.req.SignupRequest;
import com.kangaroo.sparring.domain.user.dto.req.SocialSignupCompleteRequest;
import com.kangaroo.sparring.domain.user.entity.User;
import com.kangaroo.sparring.domain.user.repository.UserRepository;
import com.kangaroo.sparring.global.email.EmailService;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserRegistrationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final HealthProfileRepository healthProfileRepository;

    @Transactional
    public void signup(SignupRequest request) {
        log.info("회원가입 시도: {}", request.getEmail());

        if (!emailService.isEmailVerified(request.getEmail())) {
            throw new CustomException(ErrorCode.EMAIL_NOT_VERIFIED);
        }

        validateDuplicateEmail(request.getEmail());
        User user = userRepository.save(createUser(request));
        HealthProfile profile = getOrCreateHealthProfile(user);
        applySignupProfile(user, profile, request);

        emailService.deleteVerifiedFlag(request.getEmail());
        log.info("회원가입 성공: userId={}, email={}", user.getId(), user.getEmail());
    }

    @Transactional
    public void completeSocialSignup(Long userId, SocialSignupCompleteRequest request) {
        User user = getUserOrThrow(userId);
        HealthProfile profile = getOrCreateHealthProfile(user);

        user.updateBirthDate(request.getBirthDate());
        user.updateGender(request.getGender());

        profile.updateProfile(
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
        healthProfileRepository.save(profile);
    }

    private void validateDuplicateEmail(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }
    }

    private User createUser(SignupRequest request) {
        return User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .username(request.getUsername())
                .build();
    }

    private void applySignupProfile(User user, HealthProfile profile, SignupRequest request) {
        user.updateBirthDate(request.getBirthDate());
        user.updateGender(request.getGender());

        profile.updateProfile(
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
        healthProfileRepository.save(profile);
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
