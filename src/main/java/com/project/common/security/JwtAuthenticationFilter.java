package com.project.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.common.exception.ErrorCode;
import com.project.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String SSE_SUBSCRIBE_PATH = "/api/notifications/subscribe";
    private static final Set<String> PUBLIC_AUTH_PATHS = Set.of(
            "/api/auth/signup",
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/logout");

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return PUBLIC_AUTH_PATHS.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        String token = resolveToken(request, authorization);

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!jwtTokenProvider.validateAccessToken(token)) {
            writeUnauthorizedResponse(response);
            return;
        }

        Long userId = jwtTokenProvider.getUserId(token);
        String role = jwtTokenProvider.getRole(token);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role)));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request, String authorization) {
        if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
            return authorization.substring(BEARER_PREFIX.length());
        }
        // EventSource(SSE)는 커스텀 헤더를 지정할 수 없어 쿼리 파라미터로 토큰을 받는다.
        if (SSE_SUBSCRIBE_PATH.equals(request.getRequestURI())) {
            String queryToken = request.getParameter("token");
            if (queryToken != null && queryToken.startsWith(BEARER_PREFIX)) {
                return queryToken.substring(BEARER_PREFIX.length());
            }
            return queryToken;
        }
        return null;
    }

    private void writeUnauthorizedResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        new ObjectMapper().writeValue(
                response.getWriter(),
                ApiResponse.error(
                        ErrorCode.UNAUTHORIZED.getCode(),
                        "유효하지 않거나 만료된 Access Token입니다."));
    }
}
