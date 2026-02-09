package com.kangaroo.sparring.global.security.oauth2.user;

import com.kangaroo.sparring.global.security.oauth2.provider.GoogleOAuth2UserInfo;
import com.kangaroo.sparring.global.security.oauth2.provider.KakaoOAuth2UserInfo;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

import java.util.Map;

public final class OAuth2UserInfoFactory {

    private OAuth2UserInfoFactory() {
    }

    public static OAuth2UserInfo create(String registrationId, Map<String, Object> attributes) {
        if (registrationId == null) {
            throw new OAuth2AuthenticationException("registrationId가 필요합니다.");
        }
        return switch (registrationId.toLowerCase()) {
            case "google" -> new GoogleOAuth2UserInfo(attributes);
            case "kakao" -> new KakaoOAuth2UserInfo(attributes);
            default -> throw new OAuth2AuthenticationException("지원하지 않는 소셜 로그인입니다: " + registrationId);
        };
    }
}
