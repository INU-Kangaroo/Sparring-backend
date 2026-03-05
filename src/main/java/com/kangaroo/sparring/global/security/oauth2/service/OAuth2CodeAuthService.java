package com.kangaroo.sparring.global.security.oauth2.service;

import com.kangaroo.sparring.domain.user.dto.req.OAuth2CodeRequest;
import com.kangaroo.sparring.domain.user.dto.res.AuthResponse;
import com.kangaroo.sparring.domain.user.entity.User;
import com.kangaroo.sparring.domain.user.service.oauth2.OAuth2UserService;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import com.kangaroo.sparring.global.security.jwt.JwtUtil;
import com.kangaroo.sparring.global.security.oauth2.user.OAuth2UserInfo;
import com.kangaroo.sparring.global.security.oauth2.user.OAuth2UserInfoFactory;
import com.kangaroo.sparring.global.security.oauth2.service.provider.OAuth2ProviderClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2CodeAuthService {

    private final OAuth2UserService oauth2UserService;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final List<OAuth2ProviderClient> providerClients;

    @Transactional
    public AuthResponse loginWithAuthorizationCode(String provider, OAuth2CodeRequest request) {
        OAuth2ProviderClient client = resolveProviderClient(provider);
        log.info("OAuth2 code login start: provider={}", client.getProvider());

        String accessToken = client.resolveAccessToken(request);
        Map<String, Object> userAttributes = client.fetchUserInfo(accessToken);
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.create(client.getProvider(), userAttributes);

        User user = oauth2UserService.processOAuth2User(userInfo);
        user.updateLastLogin();

        String jwtAccessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail());
        String jwtRefreshToken = jwtUtil.generateRefreshToken(user.getId());
        refreshTokenService.saveRefreshToken(user.getId(), jwtRefreshToken);

        return AuthResponse.of(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                jwtAccessToken,
                jwtRefreshToken
        );
    }

    private OAuth2ProviderClient resolveProviderClient(String provider) {
        if (provider == null || provider.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        String normalized = provider.trim().toLowerCase(Locale.ROOT);
        return providerClients.stream()
                .filter(client -> client.getProvider().equalsIgnoreCase(normalized))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT));
    }
}
