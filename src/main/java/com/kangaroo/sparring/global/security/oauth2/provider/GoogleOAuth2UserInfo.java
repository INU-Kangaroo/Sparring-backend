package com.kangaroo.sparring.global.security.oauth2.provider;

import com.kangaroo.sparring.domain.user.entity.Gender;
import com.kangaroo.sparring.global.security.oauth2.user.OAuth2UserInfo;

import java.time.LocalDate;
import java.util.Map;

public class GoogleOAuth2UserInfo implements OAuth2UserInfo {

    private final Map<String, Object> attributes;

    public GoogleOAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getProviderId() {
        return (String) attributes.get("sub");
    }

    @Override
    public String getProvider() {
        return "GOOGLE";
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    @Override
    public String getName() {
        return (String) attributes.get("name");
    }

    @Override
    public String getProfileImageUrl() {
        return (String) attributes.get("picture");
    }

    @Override
    public LocalDate getBirthDate() {
        // Google은 기본 scope에서 생년월일을 제공하지 않음
        // People API 연동 필요하거나 null 반환
        return null;
    }

    @Override
    public Gender getGender() {
        // Google은 기본 scope에서 성별을 제공하지 않음
        return null;
    }
}