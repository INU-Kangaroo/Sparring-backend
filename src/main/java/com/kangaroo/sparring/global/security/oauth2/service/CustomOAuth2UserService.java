package com.kangaroo.sparring.global.security.oauth2.service;

import com.kangaroo.sparring.domain.user.entity.User;
import com.kangaroo.sparring.domain.user.service.OAuth2UserService; // ← 이 import 추가
import com.kangaroo.sparring.global.security.oauth2.user.CustomOAuth2User;
import com.kangaroo.sparring.global.security.oauth2.provider.GoogleOAuth2UserInfo;
import com.kangaroo.sparring.global.security.oauth2.provider.KakaoOAuth2UserInfo;
import com.kangaroo.sparring.global.security.oauth2.user.OAuth2UserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final OAuth2UserService oauth2UserService;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUserNameAttributeName();

        log.info("OAuth2 Login - Provider: {}, UserNameAttribute: {}", registrationId, userNameAttributeName);
        log.debug("OAuth2 User Attributes: {}", oAuth2User.getAttributes());

        OAuth2UserInfo oAuth2UserInfo = createOAuth2UserInfo(registrationId, oAuth2User.getAttributes());
        User user = oauth2UserService.processOAuth2User(oAuth2UserInfo);

        return new CustomOAuth2User(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                Collections.emptyList(),
                oAuth2User.getAttributes(),
                userNameAttributeName
        );
    }

    private OAuth2UserInfo createOAuth2UserInfo(String registrationId, Map<String, Object> attributes) {
        return switch (registrationId.toLowerCase()) {
            case "google" -> new GoogleOAuth2UserInfo(attributes);
            case "kakao" -> new KakaoOAuth2UserInfo(attributes);
            default -> throw new OAuth2AuthenticationException("지원하지 않는 소셜 로그인입니다: " + registrationId);
        };
    }
}