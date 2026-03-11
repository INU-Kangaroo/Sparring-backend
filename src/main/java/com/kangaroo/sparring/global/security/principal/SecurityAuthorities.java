package com.kangaroo.sparring.global.security.principal;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

public final class SecurityAuthorities {

    private static final String ROLE_USER = "ROLE_USER";

    private SecurityAuthorities() {
    }

    public static List<GrantedAuthority> userAuthorities() {
        return List.of(new SimpleGrantedAuthority(ROLE_USER));
    }
}
