package com.kangaroo.sparring.domain.user.service;

import com.kangaroo.sparring.domain.user.dto.res.AuthResponse;
import com.kangaroo.sparring.domain.user.dto.req.LoginRequest;
import com.kangaroo.sparring.domain.user.dto.req.SignupRequest;
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

    @Transactional
    public void signup(SignupRequest request) {
        log.info("회원가입 시도: {}", request.getEmail());

        // 이메일 인증 확인
        if (!emailService.isEmailVerified(request.getEmail())) {
            throw new CustomException(ErrorCode.EMAIL_NOT_VERIFIED);
        }

        validateDuplicateEmail(request.getEmail());
        User user = userRepository.save(createUser(request));

        // 인증 플래그 삭제
        emailService.deleteVerifiedFlag(request.getEmail());

        log.info("회원가입 성공: userId={}, email={}", user.getId(), user.getEmail());
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

    /**
     * 액세스 토큰 갱신
     */
    @Transactional
    public AuthResponse refreshAccessToken(String refreshToken) {
        // 리프레시 토큰 검증
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

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
        if (!jwtUtil.validateToken(accessToken)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
        
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
