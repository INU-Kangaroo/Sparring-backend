package com.kangaroo.sparring.domain.user.service.auth;

import com.kangaroo.sparring.domain.user.entity.User;
import com.kangaroo.sparring.domain.user.repository.UserRepository;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.security.SecureRandom;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final EmailAsyncSender emailAsyncSender;
    private final RedisTemplate<String, String> redisTemplate;
    private final UserRepository userRepository;

    private static final String VERIFICATION_PREFIX = "email:verification:";
    private static final String LATEST_PREFIX = "email:verification:latest:";
    private static final String VERIFIED_PREFIX = "email:verified:";
    private static final int CODE_EXPIRATION_MINUTES = 5;
    private static final int VERIFIED_EXPIRATION_MINUTES = 30;
    private static final String RESEND_COOLDOWN_PREFIX = "email:cooldown:";
    private static final int RESEND_COOLDOWN_SECONDS = 60; // 1분
    private static final SecureRandom CODE_RANDOM = new SecureRandom();

    /**
     * 이메일 인증코드 발송
     */
    public EmailVerificationResult sendVerificationCode(String email, Long userId) {
        return sendVerification(email, userId, "email verification sent");
    }

    /**
     * 이메일 인증코드 재발송
     */
    public EmailVerificationResult resendVerificationCode(String email, Long userId) {
        return sendVerification(email, userId, "email verification resent");
    }

    /**
     * 인증코드 검증
     */
    public EmailVerificationResult verifyCode(String verificationId, String code) {
        String savedCode = redisTemplate.opsForValue().get(codeKey(verificationId));
        if (savedCode == null) {
            throw new CustomException(ErrorCode.VERIFICATION_NOT_FOUND);
        }

        if (!savedCode.equals(code)) {
            throw new CustomException(ErrorCode.INVALID_VERIFICATION_CODE);
        }

        String email = redisTemplate.opsForValue().get(emailKey(verificationId));
        if (email == null) {
            deleteVerification(verificationId, null);
            throw new CustomException(ErrorCode.VERIFICATION_NOT_FOUND);
        }

        String userIdValue = redisTemplate.opsForValue().get(userKey(verificationId));
        Long userId = userIdValue != null ? Long.valueOf(userIdValue) : null;

        if (userId == null) {
            redisTemplate.opsForValue().set(
                    VERIFIED_PREFIX + email,
                    "true",
                    VERIFIED_EXPIRATION_MINUTES,
                    TimeUnit.MINUTES
            );
        }

        deleteVerification(verificationId, email);

        log.info("email verification success: email={}, verificationId={}", email, verificationId);
        return EmailVerificationResult.forVerify(email, userId);
    }

    /**
     * 이메일 인증 여부 확인 (회원가입용)
     */
    public boolean isEmailVerified(String email) {
        String verified = redisTemplate.opsForValue().get(VERIFIED_PREFIX + email);
        return "true".equals(verified);
    }

    /**
     * 인증 완료 플래그 삭제
     */
    public void deleteVerifiedFlag(String email) {
        redisTemplate.delete(VERIFIED_PREFIX + email);
    }

    private void validateEmailForVerification(String email, Long userId) {
        Optional<User> existingUser = userRepository.findByEmail(email);
        if (existingUser.isEmpty()) {
            return;
        }
        if (userId != null && existingUser.get().getId().equals(userId)) {
            return;
        }
        throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
    }

    private void saveVerification(String verificationId, String email, String code, Long userId) {
        String latestKey = latestKey(email);
        String previousId = redisTemplate.opsForValue().get(latestKey);
        if (previousId != null) {
            deleteVerification(previousId, email);
        }

        redisTemplate.opsForValue().set(
                codeKey(verificationId),
                code,
                CODE_EXPIRATION_MINUTES,
                TimeUnit.MINUTES
        );
        redisTemplate.opsForValue().set(
                emailKey(verificationId),
                email,
                CODE_EXPIRATION_MINUTES,
                TimeUnit.MINUTES
        );
        if (userId != null) {
            redisTemplate.opsForValue().set(
                    userKey(verificationId),
                    String.valueOf(userId),
                    CODE_EXPIRATION_MINUTES,
                    TimeUnit.MINUTES
            );
        }

        redisTemplate.opsForValue().set(
                latestKey,
                verificationId,
                CODE_EXPIRATION_MINUTES,
                TimeUnit.MINUTES
        );
    }

    private EmailVerificationResult sendVerification(String email, Long userId, String logMessage) {
        enforceResendCooldown(email);
        validateEmailForVerification(email, userId);

        String code = generateRandomCode();
        String verificationId = generateVerificationId();
        saveVerification(verificationId, email, code, userId);
        startResendCooldown(email);
        emailAsyncSender.sendVerificationCode(email, code);

        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(CODE_EXPIRATION_MINUTES);
        log.info("{}: email={}, verificationId={}", logMessage, email, verificationId);
        return EmailVerificationResult.forSend(verificationId, email, expiresAt);
    }

    private void enforceResendCooldown(String email) {
        String cooldownKey = RESEND_COOLDOWN_PREFIX + email;
        String cooldown = redisTemplate.opsForValue().get(cooldownKey);
        if (cooldown != null) {
            throw new CustomException(ErrorCode.TOO_MANY_REQUESTS);
        }
    }

    private void startResendCooldown(String email) {
        String cooldownKey = RESEND_COOLDOWN_PREFIX + email;
        redisTemplate.opsForValue().set(
                cooldownKey,
                "true",
                RESEND_COOLDOWN_SECONDS,
                TimeUnit.SECONDS
        );
    }

    private void deleteVerification(String verificationId, String email) {
        redisTemplate.delete(codeKey(verificationId));
        redisTemplate.delete(emailKey(verificationId));
        redisTemplate.delete(userKey(verificationId));
        if (email != null) {
            redisTemplate.delete(latestKey(email));
        }
    }

    private String codeKey(String verificationId) {
        return VERIFICATION_PREFIX + verificationId + ":code";
    }

    private String emailKey(String verificationId) {
        return VERIFICATION_PREFIX + verificationId + ":email";
    }

    private String userKey(String verificationId) {
        return VERIFICATION_PREFIX + verificationId + ":user";
    }

    private String latestKey(String email) {
        return LATEST_PREFIX + email;
    }

    private String generateVerificationId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 6자리 랜덤 코드 생성
     */
    private String generateRandomCode() {
        return String.format("%06d", CODE_RANDOM.nextInt(1_000_000));
    }

}
