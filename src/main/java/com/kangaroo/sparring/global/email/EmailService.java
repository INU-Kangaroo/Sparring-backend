package com.kangaroo.sparring.global.email;

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

import java.util.Random;
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

    private static final String CODE_PREFIX = "email:code:";
    private static final String VERIFIED_PREFIX = "email:verified:";
    private static final int CODE_EXPIRATION_MINUTES = 5;
    private static final int VERIFIED_EXPIRATION_MINUTES = 30;
    private static final String RESEND_COOLDOWN_PREFIX = "email:cooldown:";
    private static final int RESEND_COOLDOWN_SECONDS = 60; // 1분

    /**
     * 이메일 인증코드 발송
     */
    public void sendVerificationCode(String email) {
        // 중복 이메일 체크
        if (userRepository.existsByEmail(email)) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }

        // 6자리 랜덤 코드 생성
        String code = generateRandomCode();

        // Redis에 저장 (5분)
        redisTemplate.opsForValue().set(
                CODE_PREFIX + email,
                code,
                CODE_EXPIRATION_MINUTES,
                TimeUnit.MINUTES
        );

        // 이메일 발송
        sendEmail(email, code);

        log.info("이메일 인증코드 발송 완료: email={}, code={}", email, code);
    }

    /**
     * 이메일 인증코드 재발송
     */
    public void resendVerificationCode(String email) {
        // 쿨다운 체크
        String cooldownKey = RESEND_COOLDOWN_PREFIX + email;
        String cooldown = redisTemplate.opsForValue().get(cooldownKey);

        if (cooldown != null) {
            throw new CustomException(ErrorCode.TOO_MANY_REQUESTS);
        }

        // 중복 이메일 체크
        if (userRepository.existsByEmail(email)) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }

        // 새 인증코드 생성
        String code = generateRandomCode();

        // Redis에 저장 (5분)
        redisTemplate.opsForValue().set(
                CODE_PREFIX + email,
                code,
                CODE_EXPIRATION_MINUTES,
                TimeUnit.MINUTES
        );

        // 쿨다운 설정 (1분)
        redisTemplate.opsForValue().set(
                cooldownKey,
                "true",
                RESEND_COOLDOWN_SECONDS,
                TimeUnit.SECONDS
        );

        // 이메일 발송
        sendEmail(email, code);

        log.info("이메일 인증코드 재발송 완료: email={}, code={}", email, code);
    }

    /**
     * 인증코드 검증
     */
    public void verifyCode(String email, String code) {
        String savedCode = redisTemplate.opsForValue().get(CODE_PREFIX + email);

        if (savedCode == null) {
            throw new CustomException(ErrorCode.EXPIRED_VERIFICATION_CODE);
        }

        if (!savedCode.equals(code)) {
            throw new CustomException(ErrorCode.INVALID_VERIFICATION_CODE);
        }

        // 인증 완료 플래그 생성 (30분)
        redisTemplate.opsForValue().set(
                VERIFIED_PREFIX + email,
                "true",
                VERIFIED_EXPIRATION_MINUTES,
                TimeUnit.MINUTES
        );

        // 사용한 코드 삭제
        redisTemplate.delete(CODE_PREFIX + email);

        log.info("이메일 인증 완료: email={}", email);
    }

    /**
     * 이메일 인증 여부 확인
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