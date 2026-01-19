package com.kangaroo.sparring.global.security.oauth2.handler;

import com.kangaroo.sparring.global.security.jwt.JwtUtil;
import com.kangaroo.sparring.global.security.oauth2.user.CustomOAuth2User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

import com.kangaroo.sparring.global.security.oauth2.service.RefreshTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    @Value("${oauth2.redirect.success-url}")
    private String frontendRedirectUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();

        Long userId = oAuth2User.getUserId();
        String email = oAuth2User.getEmail();
        String username = oAuth2User.getUsername();

        log.info("OAuth2 Login Success - UserId: {}, Email: {}, Username: {}", userId, email, username);

        String accessToken = jwtUtil.generateAccessToken(userId, email);
        String refreshToken = jwtUtil.generateRefreshToken(userId);
        
        // Redis에 리프레시 토큰 저장
        refreshTokenService.saveRefreshToken(userId, refreshToken);

        String targetUrl = UriComponentsBuilder.fromUriString(frontendRedirectUrl)
                .queryParam("accessToken", accessToken)
                .queryParam("refreshToken", refreshToken)
                .queryParam("userId", userId)
                .queryParam("email", email)
                .queryParam("username", username)
                .build()
                .toUriString();

        log.info("Redirecting to frontend: {}", targetUrl);

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
