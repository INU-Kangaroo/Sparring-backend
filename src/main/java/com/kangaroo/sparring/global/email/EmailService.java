package com.kangaroo.sparring.global.email;

import com.kangaroo.sparring.domain.user.entity.User;
import com.kangaroo.sparring.domain.user.repository.UserRepository;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final RedisTemplate<String, String> redisTemplate;
    private final UserRepository userRepository;

    @Value("${spring.mail.username}")
    private String fromEmail;

    private static final String VERIFICATION_PREFIX = "email:verification:";
    private static final String LATEST_PREFIX = "email:verification:latest:";
    private static final String VERIFIED_PREFIX = "email:verified:";
    private static final int CODE_EXPIRATION_MINUTES = 5;
    private static final int VERIFIED_EXPIRATION_MINUTES = 30;
    private static final String RESEND_COOLDOWN_PREFIX = "email:cooldown:";
    private static final int RESEND_COOLDOWN_SECONDS = 60; // 1분

    /**
     * 이메일 인증코드 발송
     */
    public EmailVerificationResult sendVerificationCode(String email, Long userId) {
        String cooldownKey = RESEND_COOLDOWN_PREFIX + email;
        String cooldown = redisTemplate.opsForValue().get(cooldownKey);
        if (cooldown != null) {
            throw new CustomException(ErrorCode.TOO_MANY_REQUESTS);
        }

        validateEmailForVerification(email, userId);

        String code = generateRandomCode();
        String verificationId = generateVerificationId();
        saveVerification(verificationId, email, code, userId);

        redisTemplate.opsForValue().set(
                cooldownKey,
                "true",
                RESEND_COOLDOWN_SECONDS,
                TimeUnit.SECONDS
        );

        sendEmail(email, code);

        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(CODE_EXPIRATION_MINUTES);
        log.info("email verification sent: email={}, verificationId={}", email, verificationId);
        return EmailVerificationResult.forSend(verificationId, email, expiresAt);
    }

    /**
     * 이메일 인증코드 재발송
     */
    public EmailVerificationResult resendVerificationCode(String email, Long userId) {
        String cooldownKey = RESEND_COOLDOWN_PREFIX + email;
        String cooldown = redisTemplate.opsForValue().get(cooldownKey);
        if (cooldown != null) {
            throw new CustomException(ErrorCode.TOO_MANY_REQUESTS);
        }

        validateEmailForVerification(email, userId);

        String code = generateRandomCode();
        String verificationId = generateVerificationId();
        saveVerification(verificationId, email, code, userId);

        redisTemplate.opsForValue().set(
                cooldownKey,
                "true",
                RESEND_COOLDOWN_SECONDS,
                TimeUnit.SECONDS
        );

        sendEmail(email, code);

        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(CODE_EXPIRATION_MINUTES);
        log.info("email verification resent: email={}, verificationId={}", email, verificationId);
        return EmailVerificationResult.forSend(verificationId, email, expiresAt);
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
        Random random = new Random();
        return String.format("%06d", random.nextInt(1000000));
    }

    /**
     * 이메일 발송
     */
    private void sendEmail(String toEmail, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("[Sparring] 이메일 인증코드");
            message.setText(
                    "안녕하세요. Sparring입니다.\n\n" +
                            "아래 인증코드를 입력하여 이메일 인증을 완료해주세요.\n\n" +
                            "인증코드: " + code + "\n\n" +
                            "이 코드는 5분간 유효합니다.\n\n" +
                            "감사합니다."
            );

            mailSender.send(message);
            log.info("이메일 발송 완료: {}", toEmail);

        } catch (Exception e) {
            log.error("이메일 발송 실패: {}", toEmail, e);
            throw new RuntimeException("이메일 발송에 실패했습니다.", e);
        }
    }
}
