package com.kangaroo.sparring.domain.user.service;

import com.kangaroo.sparring.domain.healthprofile.entity.HealthProfile;
import com.kangaroo.sparring.domain.healthprofile.repository.HealthProfileRepository;
import com.kangaroo.sparring.domain.user.dto.req.LoginRequest;
import com.kangaroo.sparring.domain.user.dto.req.SignupRequest;
import com.kangaroo.sparring.domain.user.dto.req.SocialSignupCompleteRequest;
import com.kangaroo.sparring.domain.user.dto.req.UpdateUserProfileRequest;
import com.kangaroo.sparring.domain.user.dto.res.AuthResponse;
import com.kangaroo.sparring.domain.user.dto.res.UserProfileResponse;
import com.kangaroo.sparring.domain.user.entity.User;
import com.kangaroo.sparring.domain.user.repository.UserRepository;
import com.kangaroo.sparring.global.email.EmailService;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import com.kangaroo.sparring.global.security.jwt.JwtUtil;
import com.kangaroo.sparring.global.security.oauth2.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final EmailService emailService;
    private final HealthProfileRepository healthProfileRepository;

    @Transactional
    public void signup(SignupRequest request) {
        log.info("회원가입 시도: {}", request.getEmail());

        // 이메일 인증 확인
        if (!emailService.isEmailVerified(request.getEmail())) {
            throw new CustomException(ErrorCode.EMAIL_NOT_VERIFIED);
        }

        validateDuplicateEmail(request.getEmail());
        User user = userRepository.save(createUser(request));
        HealthProfile profile = getOrCreateHealthProfile(user);
        applySignupProfile(user, profile, request);

        // 인증 플래그 삭제
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

    // 이메일 중복 체크
    private void validateDuplicateEmail(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }
    }

    // 사용자 생성
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

    private AuthResponse generateAuthResponse(User user) {
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());
        
        // Redis에 리프레시 토큰 저장
        refreshTokenService.saveRefreshToken(user.getId(), refreshToken);
        
        return AuthResponse.of(
            user.getId(), 
            user.getEmail(), 
            user.getUsername(), 
            accessToken,
            refreshToken
        );
    }

    public User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    public UserProfileResponse getProfile(Long userId) {
        User user = getUserOrThrow(userId);
        HealthProfile profile = healthProfileRepository.findByUserId(userId).orElse(null);
        return UserProfileResponse.of(user, profile);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("로그인 시도: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        if (!user.getIsActive() || user.isDeleted()) {
            throw new CustomException(ErrorCode.INACTIVE_USER);
        }

        user.updateLastLogin();

        log.info("로그인 성공: userId={}", user.getId());
        return generateAuthResponse(user); // 리프레시 토큰 포함
    }

    @Transactional
    public void updateEmail(Long userId, String email) {
        User user = getUserOrThrow(userId);
        validateDuplicateEmail(email);
        user.updateEmail(email);
        log.info("이메일 변경 완료: userId={}, email={}", userId, email);
    }

    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateUserProfileRequest request) {
        User user = getUserOrThrow(userId);
        HealthProfile profile = getOrCreateHealthProfile(user);

        // username은 User 엔티티에만 존재
        if (request.getUsername() != null) {
            user.updateUsername(request.getUsername());
        }

        // birthDate는 User에도 동기화 (HealthProfile을 주 저장소로 사용하되, User는 빠른 조회를 위해 캐싱)
        if (request.getBirthDate() != null) {
            user.updateBirthDate(request.getBirthDate());
        }

        // height, weight는 HealthProfile에만 업데이트 (BMI 자동 계산됨)
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

    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword, String newPasswordConfirm) {
        if (newPassword == null || !newPassword.equals(newPasswordConfirm)) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD_CONFIRM);
        }

        User user = getUserOrThrow(userId);
        if (user.isSocialUser()) {
            throw new CustomException(ErrorCode.PASSWORD_CHANGE_NOT_ALLOWED);
        }

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        user.updatePassword(passwordEncoder.encode(newPassword));
        log.info("비밀번호 변경 완료: userId={}", userId);
    }

    @Transactional
    public void deleteAccount(Long userId, String password) {
        User user = getUserOrThrow(userId);

        if (!user.isSocialUser()) {
            if (password == null || password.isBlank()) {
                throw new CustomException(ErrorCode.INVALID_PASSWORD);
            }
            if (!passwordEncoder.matches(password, user.getPassword())) {
                throw new CustomException(ErrorCode.INVALID_PASSWORD);
            }
        }

        user.deactivate();
        log.info("회원 탈퇴 처리 완료: userId={}", userId);
    }

    private HealthProfile getOrCreateHealthProfile(User user) {
        return healthProfileRepository.findByUserId(user.getId())
                .orElseGet(() -> healthProfileRepository.save(HealthProfile.builder().user(user).build()));
    }

    /**
     * 액세스 토큰 갱신
     */
    @Transactional
    public AuthResponse refreshAccessToken(String refreshToken) {
        // 리프레시 토큰 검증
        jwtUtil.validateTokenOrThrow(refreshToken);

        // 토큰 타입 확인
        String tokenType = jwtUtil.getTokenType(refreshToken);
        if (!"refresh".equals(tokenType)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        // 사용자 ID 추출
        Long userId = jwtUtil.getUserIdFromToken(refreshToken);

        // Redis에 저장된 리프레시 토큰과 비교
        if (!refreshTokenService.validateRefreshToken(userId, refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!user.getIsActive() || user.isDeleted()) {
            throw new CustomException(ErrorCode.INACTIVE_USER);
        }

        // 새 액세스 토큰 발급
        String newAccessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail());

        String newRefreshToken = jwtUtil.generateRefreshToken(user.getId());

        refreshTokenService.saveRefreshToken(user.getId(), newRefreshToken);

        log.info("액세스 토큰 갱신 성공: userId={}", userId);

        return AuthResponse.of(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                newAccessToken,
                newRefreshToken
        );
    }

    /**
     * 로그아웃
     */
    @Transactional
    public void logout(String accessToken) {
        // 액세스 토큰 검증
        jwtUtil.validateTokenOrThrow(accessToken);
        
        // 토큰 타입 확인 (액세스 토큰인지 검증)
        String tokenType = jwtUtil.getTokenType(accessToken);
        if (!"access".equals(tokenType)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
        
        Long userId = jwtUtil.getUserIdFromToken(accessToken);
        refreshTokenService.deleteRefreshToken(userId);
        log.info("로그아웃 성공: userId={}", userId);
    }
}
