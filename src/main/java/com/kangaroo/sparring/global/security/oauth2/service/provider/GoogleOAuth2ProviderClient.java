package com.kangaroo.sparring.global.security.oauth2.service.provider;

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
public class GoogleOAuth2ProviderClient {
    private static final ParameterizedTypeReference<Map<String, Object>> MAP_RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestTemplate restTemplate;

    @Value("${spring.oauth2.google.client-id:}")
    private String googleDefaultClientId;

    @Value("${spring.oauth2.google.web-client-id:${spring.oauth2.google.client-id:}}")
    private String googleWebClientId;

    @Value("${spring.oauth2.google.ios-client-id:${spring.oauth2.google.client-id:}}")
    private String googleIosClientId;

    @Value("${spring.oauth2.google.web-redirect-uri:}")
    private String googleWebRedirectUri;

    @Value("${spring.oauth2.google.ios-redirect-uri:}")
    private String googleIosRedirectUri;

    @Value("${spring.oauth2.google.client-secret:}")
    private String googleClientSecret;

    @Value("${spring.oauth2.google.token-uri}")
    private String googleTokenUri;

    @Value("${spring.oauth2.google.user-info-uri}")
    private String googleUserInfoUri;

    @Value("${spring.oauth2.google.token-info-uri}")
    private String googleTokenInfoUri;

    @Value("${spring.oauth2.google.sdk-redirect-uri:}")
    private String googleSdkRedirectUri;

    public String exchangePkceCode(String code, String redirectUri, String codeVerifier) {
        if (code == null || code.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "code는 필수입니다.");
        }
        if (redirectUri == null || redirectUri.isBlank()) {
            throw new CustomException(ErrorCode.OAUTH2_MISSING_REDIRECT_URI);
        }
        if (codeVerifier == null || codeVerifier.isBlank()) {
            throw new CustomException(ErrorCode.OAUTH2_MISSING_CODE_VERIFIER);
        }

        String clientId = resolveGoogleClientId(redirectUri);
        Map<String, Object> tokenResponse = exchangeAuthorizationCode(code, redirectUri, codeVerifier, clientId);
        return extractAndValidateAccessToken(tokenResponse, clientId);
    }

    public String exchangeSdkAuthCode(String code) {
        if (code == null || code.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "code는 필수입니다.");
        }

        String clientId = firstNonBlank(googleWebClientId, googleDefaultClientId);
        if (clientId == null || clientId.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Google server clientId가 없습니다.");
        }

        Map<String, Object> tokenResponse = exchangeSdkAuthorizationCode(code, clientId);
        return extractAndValidateAccessToken(tokenResponse, clientId);
    }

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

    private Map<String, Object> exchangeAuthorizationCode(
            String code,
            String redirectUri,
            String codeVerifier,
            String clientId
    ) {
        log.info("OAuth2 token exchange: provider=google, clientId={}, flow=pkce", clientId);
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("code", code);
        params.add("client_id", clientId);
        params.add("redirect_uri", redirectUri);
        params.add("code_verifier", codeVerifier);

        if (shouldAttachClientSecret(clientId)) {
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

    private Map<String, Object> exchangeSdkAuthorizationCode(String code, String clientId) {
        if (googleClientSecret == null || googleClientSecret.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Google client secret이 필요합니다.");
        }

        log.info("OAuth2 token exchange: provider=google, clientId={}, flow=sdk_server_auth_code", clientId);
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("code", code);
        params.add("client_id", clientId);
        params.add("client_secret", googleClientSecret);
        if (googleSdkRedirectUri != null && !googleSdkRedirectUri.isBlank()) {
            params.add("redirect_uri", googleSdkRedirectUri.trim());
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
            log.info("OAuth2 token exchange success: provider=google, flow=sdk_server_auth_code, status={}",
                    response.getStatusCode());
            return response.getBody();
        } catch (HttpClientErrorException ex) {
            log.warn("OAuth2 token exchange failed: provider=google, flow=sdk_server_auth_code, status={}, body={}",
                    ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
    }

    private boolean shouldAttachClientSecret(String resolvedClientId) {
        String webClientId = firstNonBlank(googleWebClientId, googleDefaultClientId);
        return googleClientSecret != null
                && !googleClientSecret.isBlank()
                && webClientId != null
                && !webClientId.isBlank()
                && webClientId.equals(resolvedClientId);
    }

    private String resolveGoogleClientId(String redirectUri) {
        String clientId = resolveGoogleClientIdByRedirectUri(redirectUri);
        if (clientId != null && !clientId.isBlank()) {
            return clientId;
        }
        throw new CustomException(ErrorCode.INVALID_INPUT, "redirectUri에 매핑되는 Google clientId가 없습니다.");
    }

    private String extractAndValidateAccessToken(Map<String, Object> tokenResponse, String expectedClientId) {
        String accessToken = tokenResponse != null ? (String) tokenResponse.get("access_token") : null;
        String idToken = tokenResponse != null ? (String) tokenResponse.get("id_token") : null;
        validateGoogleIdToken(idToken, expectedClientId);

        if (accessToken == null || accessToken.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
        return accessToken;
    }

    private String resolveGoogleClientIdByRedirectUri(String redirectUri) {
        if (redirectUri == null || redirectUri.isBlank()) {
            return null;
        }
        String requestRedirectUri = normalizeRedirectUri(redirectUri);
        String configuredWebRedirectUri = normalizeRedirectUri(googleWebRedirectUri);
        String configuredIosRedirectUri = normalizeRedirectUri(googleIosRedirectUri);
        if (requestRedirectUri == null) {
            return null;
        }
        if (configuredIosRedirectUri != null && requestRedirectUri.equals(configuredIosRedirectUri)) {
            return firstNonBlank(googleIosClientId, googleDefaultClientId);
        }
        if (configuredWebRedirectUri != null && requestRedirectUri.equals(configuredWebRedirectUri)) {
            return firstNonBlank(googleWebClientId, googleDefaultClientId);
        }
        return null;
    }

    private String normalizeRedirectUri(String redirectUri) {
        if (redirectUri == null) {
            return null;
        }
        String normalized = redirectUri.trim();
        if (normalized.isBlank()) {
            return null;
        }
        while (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private void validateGoogleIdToken(String idToken, String expectedClientId) {
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
            if (!expectedClientId.equals(aud)) {
                throw new CustomException(ErrorCode.INVALID_TOKEN);
            }
            if (!"accounts.google.com".equals(iss) && !"https://accounts.google.com".equals(iss)) {
                throw new CustomException(ErrorCode.INVALID_TOKEN);
            }
            if (body.get("azp") != null && !expectedClientId.equals(String.valueOf(body.get("azp")))) {
                log.info("Google id_token azp differs from expected clientId. azp={}, expectedClientId={}",
                        body.get("azp"), expectedClientId);
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
