package com.kangaroo.sparring.domain.auth.service;

import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static com.kangaroo.sparring.global.support.LogMaskingSupport.maskEmail;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailAsyncSender {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async("mailExecutor")
    public CompletableFuture<Void> sendVerificationCode(String toEmail, String code) {
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
            log.info("이메일 발송 완료: {}", maskEmail(toEmail));
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("이메일 비동기 발송 실패: {}", maskEmail(toEmail), e);
            throw new CompletionException(new CustomException(ErrorCode.EMAIL_SEND_FAILED));
        }
    }
}
