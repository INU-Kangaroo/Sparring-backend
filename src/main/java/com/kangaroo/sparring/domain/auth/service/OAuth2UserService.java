package com.kangaroo.sparring.domain.auth.service;

import com.kangaroo.sparring.domain.user.type.SocialProvider;
import com.kangaroo.sparring.domain.user.entity.User;
import com.kangaroo.sparring.domain.user.repository.UserRepository;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import com.kangaroo.sparring.global.security.oauth2.user.OAuth2UserInfo;
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
public class OAuth2UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User processOAuth2User(OAuth2UserInfo userInfo) {
        log.info("Processing OAuth2 user - Provider: {}, Email: {}",
                userInfo.getProvider(), maskEmail(userInfo.getEmail()));
        if (userInfo.getEmail() == null || userInfo.getEmail().isBlank()) {
            throw new CustomException(ErrorCode.OAUTH2_EMAIL_REQUIRED);
        }
        String resolvedUsername = resolveUsername(userInfo);

        SocialProvider provider = convertToSocialProvider(userInfo.getProvider());

        return userRepository.findByEmail(userInfo.getEmail())
                .map(existingUser -> {
                    if (existingUser.getProvider() == null || existingUser.getProvider() != provider) {
                        log.warn("OAuth2 provider mismatch: userId={}, requestedProvider={}, currentProvider={}",
                                existingUser.getId(), provider, existingUser.getProvider());
                        throw new CustomException(ErrorCode.OAUTH2_PROVIDER_MISMATCH);
                    }
                    return updateOAuth2User(existingUser, userInfo, provider, resolvedUsername);
                })
                .orElseGet(() -> createOAuth2User(userInfo, provider, resolvedUsername));
    }

    private User createOAuth2User(OAuth2UserInfo userInfo, SocialProvider provider, String resolvedUsername) {
        log.info("Creating new OAuth2 user - Email: {}, Provider: {}", maskEmail(userInfo.getEmail()), provider);
        User user = User.builder()
                .email(userInfo.getEmail())
                .password(passwordEncoder.encode("OAUTH2_USER"))
                .username(resolvedUsername)
                .provider(provider)
                .providerId(userInfo.getProviderId())
                .profileImageUrl(userInfo.getProfileImageUrl())
                .birthDate(userInfo.getBirthDate())
                .gender(userInfo.getGender())
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);
        log.info("OAuth2 신규 사용자 생성 완료: userId={}, provider={}", savedUser.getId(), provider);
        return savedUser;
    }

    private User updateOAuth2User(User user, OAuth2UserInfo userInfo, SocialProvider provider, String resolvedUsername) {
        log.info("Updating existing user - Email: {}", maskEmail(userInfo.getEmail()));
        if (user.getProvider() == null) {
            user.updateProvider(provider, userInfo.getProviderId());
        }
        
        if (userInfo.getProfileImageUrl() != null) {
            user.updateProfile(user.getUsername(), userInfo.getProfileImageUrl());
        } else if (user.getUsername() == null || user.getUsername().isBlank()) {
            user.updateUsername(resolvedUsername);
        }

        if (user.getBirthDate() == null && userInfo.getBirthDate() != null) {
            user.updateBirthDate(userInfo.getBirthDate());
        }
        if (user.getGender() == null && userInfo.getGender() != null) {
            user.updateGender(userInfo.getGender());
        }

        user.updateLastLogin();
        log.info("OAuth2 기존 사용자 로그인 처리 완료: userId={}, provider={}", user.getId(), provider);
        return user;
    }

    private SocialProvider convertToSocialProvider(String provider) {
        return switch (provider.toUpperCase()) {
            case "GOOGLE" -> SocialProvider.GOOGLE;
            case "KAKAO" -> SocialProvider.KAKAO;
            default -> throw new IllegalArgumentException("알 수 없는 Provider: " + provider);
        };
    }

    private String resolveUsername(OAuth2UserInfo userInfo) {
        String name = userInfo.getName();
        if (name != null && !name.isBlank()) {
            return name.trim();
        }
        String email = userInfo.getEmail();
        if (email != null && email.contains("@")) {
            String localPart = email.substring(0, email.indexOf('@')).trim();
            if (!localPart.isBlank()) {
                return localPart;
            }
        }
        return "user";
    }
}
