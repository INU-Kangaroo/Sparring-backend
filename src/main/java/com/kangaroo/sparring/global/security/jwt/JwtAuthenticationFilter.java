package com.kangaroo.sparring.global.security.jwt;

import com.kangaroo.sparring.domain.user.entity.User;
import com.kangaroo.sparring.domain.user.repository.UserRepository;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import com.kangaroo.sparring.global.security.principal.CurrentUser;
import com.kangaroo.sparring.global.security.principal.SecurityAuthorities;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Authorization Header 의 Bearer token 을 parsing 하여 Authentication 정보를 SecurityContext 에 저장하는 역할을 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAccessDeniedHandler accessDeniedHandler;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String token = resolveToken(request);
        if (token != null) {
            try {
                jwtUtil.validateTokenOrThrow(token);

                String tokenType = jwtUtil.getTokenType(token);
                if (!"access".equals(tokenType)) {
                    authenticationEntryPoint.commence(
                            request,
                            response,
                            new InsufficientAuthenticationException("액세스 토큰이 아닙니다.")
                    );
                    return;
                }
                Long userId = jwtUtil.getUserIdFromToken(token);
                User user = userRepository.findById(userId).orElse(null);

                if (user == null) {
                    authenticationEntryPoint.commence(
                            request,
                            response,
                            new InsufficientAuthenticationException("사용자를 찾을 수 없습니다.")
                    );
                    return;
                }

                if (!user.getIsActive() || user.isDeleted()) {
                    accessDeniedHandler.handle(
                            request,
                            response,
                            new AccessDeniedException("비활성 또는 삭제된 사용자입니다.")
                    );
                    return;
                }

                UsernamePasswordAuthenticationToken authentication = buildAuthentication(user, request);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (CustomException e) {
                if (e.getErrorCode() == ErrorCode.EXPIRED_TOKEN) {
                    authenticationEntryPoint.commence(
                            request,
                            response,
                            new InsufficientAuthenticationException("만료된 토큰입니다.")
                    );
                } else {
                    authenticationEntryPoint.commence(
                            request,
                            response,
                            new InsufficientAuthenticationException("유효하지 않은 토큰입니다.")
                    );
                }
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    private UsernamePasswordAuthenticationToken buildAuthentication(User user, HttpServletRequest request) {
        CurrentUser principal = CurrentUser.fromJwt(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                SecurityAuthorities.userAuthorities()
        );

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        return authentication;
    }
}
