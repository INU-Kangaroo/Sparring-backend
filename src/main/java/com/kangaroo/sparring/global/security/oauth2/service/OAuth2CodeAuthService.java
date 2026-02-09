package com.kangaroo.sparring.global.security.oauth2.service;

import com.kangaroo.sparring.domain.user.dto.req.OAuth2CodeRequest;
import com.kangaroo.sparring.domain.user.dto.res.AuthResponse;
import com.kangaroo.sparring.domain.user.entity.User;
import com.kangaroo.sparring.domain.user.service.OAuth2UserService;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import com.kangaroo.sparring.global.security.jwt.JwtUtil;
import com.kangaroo.sparring.global.security.oauth2.user.OAuth2UserInfo;
import com.kangaroo.sparring.global.security.oauth2.user.OAuth2UserInfoFactory;
import org.springframework.beans.factory.annotation.Value;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2CodeAuthService {

    private final RestTemplate restTemplate;
    private final OAuth2UserService oauth2UserService;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    @Value("${spring.oauth2.google.client-id}")
    private String googleClientId;

    @Value("${spring.oauth2.google.client-secret}")
    private String googleClientSecret;

    @Value("${spring.oauth2.google.token-uri}")
    private String googleTokenUri;

    @Value("${spring.oauth2.google.user-info-uri}")
    private String googleUserInfoUri;

    @Value("${spring.oauth2.google.token-info-uri}")
    private String googleTokenInfoUri;

    @Value("${spring.oauth2.kakao.client-id}")
    private String kakaoClientId;

    @Value("${spring.oauth2.kakao.token-uri}")
    private String kakaoTokenUri;

    @Value("${spring.oauth2.kakao.user-info-uri}")
    private String kakaoUserInfoUri;

    @Value("${spring.oauth2.kakao.access-token-info-uri}")
    private String kakaoAccessTokenInfoUri;

    @Value("${spring.oauth2.kakao.app-id}")
    private String kakaoAppId;

    @Transactional
    public AuthResponse loginWithAuthorizationCode(String provider, OAuth2CodeRequest request) {
        if (!"google".equalsIgnoreCase(provider) && !"kakao".equalsIgnoreCase(provider)) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        log.info("OAuth2 code login start: provider={}", provider);

        String accessToken;
        if ("kakao".equalsIgnoreCase(provider)) {
            if (request.getAccessToken() == null || request.getAccessToken().isBlank()) {
                throw new CustomException(ErrorCode.INVALID_INPUT);
            }
            accessToken = request.getAccessToken();
            log.info("Using Kakao access token from client.");
            validateKakaoAccessToken(accessToken);
        } else {
            if (request.getCode() == null || request.getCode().isBlank()) {
                throw new CustomException(ErrorCode.INVALID_INPUT);
            }
            Map<String, Object> tokenResponse = exchangeAuthorizationCode(provider, request);
            accessToken = tokenResponse != null ? (String) tokenResponse.get("access_token") : null;
            if ("google".equalsIgnoreCase(provider)) {
                String idToken = tokenResponse != null ? (String) tokenResponse.get("id_token") : null;
                validateGoogleIdToken(idToken);
            }
        }
        if (accessToken == null || accessToken.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        Map<String, Object> userAttributes = fetchUserInfo(provider, accessToken);
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.create(provider, userAttributes);

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

    private Map<String, Object> exchangeAuthorizationCode(String provider, OAuth2CodeRequest request) {
        if (!"google".equalsIgnoreCase(provider) && !"kakao".equalsIgnoreCase(provider)) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        log.info("OAuth2 token exchange: provider={}, clientId={}",
                provider,
                "google".equalsIgnoreCase(provider) ? googleClientId : kakaoClientId);
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("code", request.getCode());
        params.add("client_id", "google".equalsIgnoreCase(provider) ? googleClientId : kakaoClientId);
        // RN Google Sign-In serverAuthCode 흐름은 redirect_uri/code_verifier 없이 교환 가능
        String clientSecret = "google".equalsIgnoreCase(provider) ? googleClientSecret : null;
        if (clientSecret != null && !clientSecret.isBlank()) {
            params.add("client_secret", clientSecret);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);
        String tokenUri = "google".equalsIgnoreCase(provider) ? googleTokenUri : kakaoTokenUri;

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    tokenUri,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );
            log.info("OAuth2 token exchange success: provider={}, status={}",
                    provider,
                    response.getStatusCode());
            return response.getBody();
        } catch (HttpClientErrorException ex) {
            log.warn("OAuth2 token exchange failed: status={}, body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
    }

    private Map<String, Object> fetchUserInfo(String provider, String accessToken) {
        if (!"google".equalsIgnoreCase(provider) && !"kakao".equalsIgnoreCase(provider)) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        HttpMethod method = "kakao".equalsIgnoreCase(provider)
                ? HttpMethod.POST
                : HttpMethod.GET;

        String userInfoUri = "google".equalsIgnoreCase(provider) ? googleUserInfoUri : kakaoUserInfoUri;
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    userInfoUri,
                    method,
                    entity,
                    Map.class
            );
            return response.getBody();
        } catch (HttpClientErrorException ex) {
            log.warn("OAuth2 userinfo request failed: status={}, body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
    }

    private void validateGoogleIdToken(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
        if (googleTokenInfoUri == null || googleTokenInfoUri.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        String url = UriComponentsBuilder.fromHttpUrl(googleTokenInfoUri)
                .queryParam("id_token", idToken)
                .toUriString();

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    HttpEntity.EMPTY,
                    Map.class
            );
            Map<String, Object> body = response.getBody();
            if (body == null) {
                throw new CustomException(ErrorCode.INVALID_TOKEN);
            }
            String aud = String.valueOf(body.get("aud"));
            String iss = String.valueOf(body.get("iss"));
            if (!googleClientId.equals(aud)) {
                throw new CustomException(ErrorCode.INVALID_TOKEN);
            }
            if (!"accounts.google.com".equals(iss) && !"https://accounts.google.com".equals(iss)) {
                throw new CustomException(ErrorCode.INVALID_TOKEN);
            }
            if (body.get("azp") != null && !googleClientId.equals(String.valueOf(body.get("azp")))) {
                throw new CustomException(ErrorCode.INVALID_TOKEN);
            }
            validateGoogleTokenTimes(body);
        } catch (HttpClientErrorException ex) {
            log.warn("Google id_token validation failed: status={}, body={}",
                    ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
    }

    private void validateGoogleTokenTimes(Map<String, Object> body) {
        long now = Instant.now().getEpochSecond();
        long exp = parseLongClaim(body.get("exp"));
        long iat = parseLongClaim(body.get("iat"));

        if (exp > 0 && exp < now) {
            throw new CustomException(ErrorCode.EXPIRED_TOKEN);
        }
        if (iat > 0 && iat - now > 300) { // 5분 이상 미래 토큰 차단
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
        if (iat > 0 && exp > 0 && exp <= iat) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
    }

    private long parseLongClaim(Object value) {
        if (value == null) return 0;
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void validateKakaoAccessToken(String accessToken) {
        if (kakaoAccessTokenInfoUri == null || kakaoAccessTokenInfoUri.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        if (kakaoAppId == null || kakaoAppId.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    kakaoAccessTokenInfoUri,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );
            Map<String, Object> body = response.getBody();
            if (body == null || !kakaoAppId.equals(String.valueOf(body.get("app_id")))) {
                throw new CustomException(ErrorCode.INVALID_TOKEN);
            }
        } catch (HttpClientErrorException ex) {
            log.warn("Kakao access token validation failed: status={}, body={}",
                    ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
    }
}
