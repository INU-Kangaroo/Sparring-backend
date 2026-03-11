package com.kangaroo.sparring.global.security.oauth2.service.provider;

import com.kangaroo.sparring.domain.user.dto.req.OAuth2CodeRequest;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleOAuth2ProviderClient implements OAuth2ProviderClient {
    private static final ParameterizedTypeReference<Map<String, Object>> MAP_RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestTemplate restTemplate;

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

    @Override
    public String getProvider() {
        return "google";
    }

    @Override
    public String resolveAccessToken(OAuth2CodeRequest request) {
        if (request.getCode() == null || request.getCode().isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "code는 필수입니다.");
        }
        if (request.getRedirectUri() == null || request.getRedirectUri().isBlank()) {
            throw new CustomException(ErrorCode.OAUTH2_MISSING_REDIRECT_URI);
        }
        if (request.getCodeVerifier() == null || request.getCodeVerifier().isBlank()) {
            throw new CustomException(ErrorCode.OAUTH2_MISSING_CODE_VERIFIER);
        }

        Map<String, Object> tokenResponse = exchangeAuthorizationCode(request);
        String accessToken = tokenResponse != null ? (String) tokenResponse.get("access_token") : null;
        String idToken = tokenResponse != null ? (String) tokenResponse.get("id_token") : null;
        validateGoogleIdToken(idToken);

        if (accessToken == null || accessToken.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
        return accessToken;
    }

    @Override
    public Map<String, Object> fetchUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    googleUserInfoUri,
                    HttpMethod.GET,
                    entity,
                    MAP_RESPONSE_TYPE
            );
            return response.getBody();
        } catch (HttpClientErrorException ex) {
            log.warn("Google userinfo request failed: status={}, body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
    }

    private Map<String, Object> exchangeAuthorizationCode(OAuth2CodeRequest request) {
        log.info("OAuth2 token exchange: provider=google, clientId={}", googleClientId);
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("code", request.getCode());
        params.add("client_id", googleClientId);
        params.add("redirect_uri", request.getRedirectUri());
        params.add("code_verifier", request.getCodeVerifier());
        if (googleClientSecret != null && !googleClientSecret.isBlank()) {
            params.add("client_secret", googleClientSecret);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    googleTokenUri,
                    HttpMethod.POST,
                    entity,
                    MAP_RESPONSE_TYPE
            );
            log.info("OAuth2 token exchange success: provider=google, status={}", response.getStatusCode());
            return response.getBody();
        } catch (HttpClientErrorException ex) {
            log.warn("OAuth2 token exchange failed: status={}, body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
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

        String url = UriComponentsBuilder.fromUriString(googleTokenInfoUri)
                .queryParam("id_token", idToken)
                .toUriString();

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    MAP_RESPONSE_TYPE
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
        if (iat > 0 && iat - now > 300) {
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
}
