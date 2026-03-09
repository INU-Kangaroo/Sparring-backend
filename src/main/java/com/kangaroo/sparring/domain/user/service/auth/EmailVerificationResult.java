package com.kangaroo.sparring.domain.user.service.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class EmailVerificationResult {

    private final String verificationId;
    private final String email;
    private final Long userId;
    private final LocalDateTime expiresAt;

    public static EmailVerificationResult forSend(String verificationId, String email, LocalDateTime expiresAt) {
        return new EmailVerificationResult(verificationId, email, null, expiresAt);
    }

    public static EmailVerificationResult forVerify(String email, Long userId) {
        return new EmailVerificationResult(null, email, userId, null);
    }
}
