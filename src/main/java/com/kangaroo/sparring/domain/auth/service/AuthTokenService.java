package com.kangaroo.sparring.domain.auth.service;

import com.kangaroo.sparring.domain.auth.dto.req.LoginRequest;
import com.kangaroo.sparring.domain.auth.dto.res.AuthResponse;
import com.kangaroo.sparring.domain.user.entity.User;
import com.kangaroo.sparring.domain.user.repository.UserRepository;
import com.kangaroo.sparring.domain.user.service.UserLookupService;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import com.kangaroo.sparring.global.security.jwt.JwtUtil;
import com.kangaroo.sparring.global.security.oauth2.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.kangaroo.sparring.global.support.LogMaskingSupport.maskEmail;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthTokenService {

    private static final String DUMMY_BCRYPT_HASH =
            "$2a$10$7EqJtq98hPqEX7fNZaFWoOHiM8w6RzGQKxW0fvkYl9wH14r2raXI6";

    private final UserRepository userRepository;
    private final UserLookupService userLookupService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.debug("로그인 시도: {}", maskEmail(request.getEmail()));

        User user = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (user == null) {
            // 존재하지 않는 계정도 동일한 비용의 검증을 수행해 계정 유무 추측을 어렵게 한다.
            passwordEncoder.matches(request.getPassword(), DUMMY_BCRYPT_HASH);
            log.warn("로그인 실패: 계정 또는 비밀번호 불일치");
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("로그인 실패: userId={}, 계정 또는 비밀번호 불일치", user.getId());
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        validateActiveUser(user);
        user.updateLastLogin();

        log.info("로그인 성공: userId={}", user.getId());
        return generateAuthResponse(user);
    }

    @Transactional
    public AuthResponse refreshAccessToken(String refreshToken) {
        jwtUtil.validateTokenOrThrow(refreshToken);

        String tokenType = jwtUtil.getTokenType(refreshToken);
        if (!"refresh".equals(tokenType)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        Long userId = jwtUtil.getUserIdFromToken(refreshToken);
        if (!refreshTokenService.validateRefreshToken(userId, refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        User user = userLookupService.getUserOrThrow(userId);
        validateActiveUser(user);

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

    @Transactional
    public void logout(String accessToken) {
        jwtUtil.validateTokenOrThrow(accessToken);

        String tokenType = jwtUtil.getTokenType(accessToken);
        if (!"access".equals(tokenType)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        Long userId = jwtUtil.getUserIdFromToken(accessToken);
        refreshTokenService.deleteRefreshToken(userId);
        refreshTokenService.revokeAccessTokens(userId);
        log.info("로그아웃 성공: userId={}", userId);
    }

    private AuthResponse generateAuthResponse(User user) {
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());
        refreshTokenService.saveRefreshToken(user.getId(), refreshToken);

        return AuthResponse.of(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                accessToken,
                refreshToken
        );
    }

    private void validateActiveUser(User user) {
        if (!user.getIsActive() || user.isDeleted()) {
            log.warn("로그인 실패: 비활성 사용자 userId={}", user.getId());
            throw new CustomException(ErrorCode.INACTIVE_USER);
        }
    }
}
