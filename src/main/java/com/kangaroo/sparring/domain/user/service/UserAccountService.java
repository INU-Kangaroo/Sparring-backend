package com.kangaroo.sparring.domain.user.service;

import com.kangaroo.sparring.domain.user.entity.User;
import com.kangaroo.sparring.domain.user.repository.UserRepository;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
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
public class UserAccountService {

    private final UserRepository userRepository;
    private final UserLookupService userLookupService;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public void updateEmail(Long userId, String email) {
        User user = userLookupService.getUserOrThrow(userId);
        validateDuplicateEmail(email);
        user.updateEmail(email);
        log.info("이메일 변경 완료: userId={}, email={}", userId, maskEmail(email));
    }

    public String getEmailOrThrow(Long userId) {
        return userLookupService.getUserOrThrow(userId).getEmail();
    }

    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword, String newPasswordConfirm) {
        if (newPassword == null || !newPassword.equals(newPasswordConfirm)) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD_CONFIRM);
        }

        User user = userLookupService.getUserOrThrow(userId);
        if (user.isSocialUser()) {
            throw new CustomException(ErrorCode.PASSWORD_CHANGE_NOT_ALLOWED);
        }

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        user.updatePassword(passwordEncoder.encode(newPassword));
        refreshTokenService.deleteRefreshToken(userId);
        refreshTokenService.revokeAccessTokens(userId);
        log.info("비밀번호 변경 완료: userId={}", userId);
    }

    @Transactional
    public void deleteAccount(Long userId, String password) {
        User user = userLookupService.getUserOrThrow(userId);

        if (!user.isSocialUser()) {
            if (password == null || password.isBlank()) {
                throw new CustomException(ErrorCode.INVALID_PASSWORD);
            }
            if (!passwordEncoder.matches(password, user.getPassword())) {
                throw new CustomException(ErrorCode.INVALID_PASSWORD);
            }
        }

        user.deactivate();
        refreshTokenService.deleteRefreshToken(userId);
        refreshTokenService.revokeAccessTokens(userId);
        log.info("회원 탈퇴 처리 완료: userId={}", userId);
    }

    private void validateDuplicateEmail(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }
    }
}
