package com.kangaroo.sparring.global.security.principal;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

public class CurrentUser implements UserDetails, OAuth2User, UserIdPrincipal {

    private final Long userId;
    private final String email;
    private final String displayName;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;
    private final Map<String, Object> attributes;
    private final String nameAttributeKey;

    private CurrentUser(
            Long userId,
            String email,
            String displayName,
            String password,
            Collection<? extends GrantedAuthority> authorities,
            Map<String, Object> attributes,
            String nameAttributeKey
    ) {
        this.userId = userId;
        this.email = email;
        this.displayName = displayName;
        this.password = password;
        this.authorities = authorities;
        this.attributes = attributes;
        this.nameAttributeKey = nameAttributeKey;
    }

    public static CurrentUser fromJwt(
            Long userId,
            String email,
            String password,
            Collection<? extends GrantedAuthority> authorities
    ) {
        return new CurrentUser(userId, email, email, password, authorities, null, null);
    }

    public static CurrentUser fromOAuth2(
            Long userId,
            String email,
            String displayName,
            Collection<? extends GrantedAuthority> authorities,
            Map<String, Object> attributes,
            String nameAttributeKey
    ) {
        return new CurrentUser(userId, email, displayName, null, authorities, attributes, nameAttributeKey);
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public Long getUserId() {
        return userId;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public String getName() {
        if (attributes == null || nameAttributeKey == null) {
            return String.valueOf(userId);
        }
        Object value = attributes.get(nameAttributeKey);
        if (value == null) {
            return String.valueOf(userId);
        }
        return String.valueOf(value);
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
