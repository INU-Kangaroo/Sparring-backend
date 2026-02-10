package com.kangaroo.sparring.global.security.oauth2.service.provider;

import com.kangaroo.sparring.domain.user.dto.req.OAuth2CodeRequest;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoOAuth2ProviderClient implements OAuth2ProviderClient {

    private final RestTemplate restTemplate;

    @Value("${spring.oauth2.kakao.user-info-uri}")
    private String kakaoUserInfoUri;

    @Value("${spring.oauth2.kakao.access-token-info-uri}")
    private String kakaoAccessTokenInfoUri;

    @Value("${spring.oauth2.kakao.app-id}")
    private String kakaoAppId;

    @Override
    public String getProvider() {
        return "kakao";
    }

    @Override
    public String resolveAccessToken(OAuth2CodeRequest request) {
        if (request.getAccessToken() == null || request.getAccessToken().isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        String accessToken = request.getAccessToken();
        log.info("Using Kakao access token from client.");
        validateKakaoAccessToken(accessToken);
        return accessToken;
    }

    @Override
    public Map<String, Object> fetchUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    kakaoUserInfoUri,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );
            return response.getBody();
        } catch (HttpClientErrorException ex) {
            log.warn("Kakao userinfo request failed: status={}, body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new CustomException(ErrorCode.INVALID_TOKEN);
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
