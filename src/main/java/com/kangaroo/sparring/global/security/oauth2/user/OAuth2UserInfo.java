package com.kangaroo.sparring.global.security.oauth2.user;

import com.kangaroo.sparring.domain.user.entity.Gender;
import java.time.LocalDate;

public interface OAuth2UserInfo {
    String getProviderId();
    String getProvider();
    String getEmail();
    String getName();
    String getProfileImageUrl();
    LocalDate getBirthDate();
    Gender getGender();
}