package com.kangaroo.sparring.domain.auth.service;

import com.kangaroo.sparring.domain.healthprofile.entity.HealthProfile;
import com.kangaroo.sparring.domain.healthprofile.repository.HealthProfileRepository;
import com.kangaroo.sparring.domain.auth.dto.req.SignupRequest;
import com.kangaroo.sparring.domain.user.entity.User;
import com.kangaroo.sparring.domain.user.repository.UserRepository;
import com.kangaroo.sparring.domain.auth.service.EmailService;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.kangaroo.sparring.global.support.LogMaskingSupport.maskEmail;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserRegistrationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final HealthProfileRepository healthProfileRepository;
    @Value("${app.auth.signup.require-email-verification:true}")
    private boolean requireEmailVerification;

    @Transactional
    public void signup(SignupRequest request) {
        log.debug("회원가입 시도: {}", maskEmail(request.getEmail()));

        if (requireEmailVerification && !emailService.isEmailVerified(request.getEmail())) {
            throw new CustomException(ErrorCode.EMAIL_NOT_VERIFIED);
        }

        validateDuplicateEmail(request.getEmail());
        User user = userRepository.save(createUser(request));
        HealthProfile profile = getOrCreateHealthProfile(user);
        applySignupProfile(user, profile, request);

        if (requireEmailVerification) {
            emailService.deleteVerifiedFlag(request.getEmail());
        }
        log.info("회원가입 성공: userId={}, email={}", user.getId(), maskEmail(user.getEmail()));
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
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
        healthProfileRepository.save(profile);
    }

    private HealthProfile getOrCreateHealthProfile(User user) {
        return healthProfileRepository.findByUserId(user.getId())
                .orElseGet(() -> healthProfileRepository.save(HealthProfile.builder().user(user).build()));
    }
}
