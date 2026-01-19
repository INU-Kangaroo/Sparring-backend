package com.kangaroo.sparring.global.security.oauth2.provider;

import com.kangaroo.sparring.domain.user.entity.Gender;
import com.kangaroo.sparring.global.security.oauth2.user.OAuth2UserInfo;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class KakaoOAuth2UserInfo implements OAuth2UserInfo {

    private final Map<String, Object> attributes;
    private final Map<String, Object> kakaoAccount;
    private final Map<String, Object> properties;

    @SuppressWarnings("unchecked")
    public KakaoOAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
        this.kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        this.properties = (Map<String, Object>) attributes.get("properties");
    }

    @Override
    public String getProviderId() {
        return String.valueOf(attributes.get("id"));
    }

    @Override
    public String getProvider() {
        return "KAKAO";
    }

    @Override
    public String getEmail() {
        return kakaoAccount != null ? (String) kakaoAccount.get("email") : null;
    }

    @Override
    public String getName() {
        return properties != null ? (String) properties.get("nickname") : null;
    }

    @Override
    public String getProfileImageUrl() {
        return properties != null ? (String) properties.get("profile_image") : null;
    }

    @Override
    public LocalDate getBirthDate() {
        if (kakaoAccount == null) return null;
        
        String birthyear = (String) kakaoAccount.get("birthyear"); // "1990"
        String birthday = (String) kakaoAccount.get("birthday");   // "1225" (MMDD)
        
        if (birthyear != null && birthday != null) {
            try {
                String birthDateStr = birthyear + birthday; // "19901225"
                return LocalDate.parse(birthDateStr, DateTimeFormatter.ofPattern("yyyyMMdd"));
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    @Override
    public Gender getGender() {
        if (kakaoAccount == null) return null;
        
        String gender = (String) kakaoAccount.get("gender");
        if (gender == null) return null;
        
        return switch (gender) {
            case "male" -> Gender.MALE;
            case "female" -> Gender.FEMALE;
            default -> Gender.OTHER;
        };
    }
}