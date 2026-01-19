package com.kangaroo.sparring.global.security.oauth2.user;

import com.kangaroo.sparring.global.security.UserIdPrincipal;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

@Getter
public class CustomOAuth2User implements OAuth2User, UserIdPrincipal {

    private final Long userId;
    private final String email;
    private final String username;  // ← 실제 사용자 이름
    private final Collection<? extends GrantedAuthority> authorities;
    private final Map<String, Object> attributes;
    private final String nameAttributeKey;

    public CustomOAuth2User(
            Long userId,
            String email,
            String username,  // ← 파라미터 이름 변경
            Collection<? extends GrantedAuthority> authorities,
            Map<String, Object> attributes,
            String nameAttributeKey
    ) {
        this.userId = userId;
        this.email = email;
        this.username = username;  // ← 실제 사용자 이름 저장
        this.authorities = authorities;
        this.attributes = attributes;
        this.nameAttributeKey = nameAttributeKey;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getName() {
        Object value = attributes.get(nameAttributeKey);
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }
}
