package com.dev.lib.web.security.util;

import com.dev.lib.security.TokenService;
import com.dev.lib.security.util.SecurityContextHolder;
import com.dev.lib.security.util.UserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 校验 token，设置SecurityContextHolder上下文
 */
@Slf4j
@Order(1)
@Component
@RequiredArgsConstructor
public class AuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String token = extractToken(request);
            UserDetails userDetail = tokenService.parseToken(token);

            if (userDetail != null && !userDetail.isExpired()) {
                userDetail.setClientIp(ClientInfoExtractor.getClientIp(request));
                userDetail.setClientType(ClientInfoExtractor.getClientType(request));
                userDetail.setDeviceId(request.getHeader("X-Device-Id"));

                SecurityContextHolder.set(userDetail);
            }
        } finally {
            filterChain.doFilter(request, response);
            SecurityContextHolder.clear();
        }
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        return (bearerToken != null && bearerToken.startsWith("Bearer "))
                ? bearerToken.substring(7)
                : null;
    }
}