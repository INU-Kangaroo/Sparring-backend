package com.kangaroo.sparring.domain.user.service.auth;

import com.kangaroo.sparring.domain.user.dto.req.LoginRequest;
import com.kangaroo.sparring.domain.user.dto.res.AuthResponse;
import com.kangaroo.sparring.domain.user.entity.User;
import com.kangaroo.sparring.domain.user.repository.UserRepository;
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
public class AuthTokenService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("로그인 시도: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
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

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
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
            throw new CustomException(ErrorCode.INACTIVE_USER);
        }
    }
}
