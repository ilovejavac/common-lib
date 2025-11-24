package com.dev.lib.security.util;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 业务服务用：从 Gateway 传递的 Header 中提取用户信息
 */
@Slf4j
@Order(1)
public class UserContextFilter extends OncePerRequestFilter {

    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USERNAME = "X-Username";
    private static final String HEADER_TENANT_ID = "X-Tenant-Id";
    private static final String HEADER_CLIENT_IP = "X-Client-Ip";
    private static final String HEADER_CLIENT_TYPE = "X-Client-Type";
    private static final String HEADER_DEVICE_ID = "X-Device-Id";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String userId = request.getHeader(HEADER_USER_ID);

            if (StringUtils.hasText(userId)) {
                UserDetails userDetails = UserDetails.builder()
                        .id(Long.parseLong(userId))
                        .username(request.getHeader(HEADER_USERNAME))
                        .tenant(parseLong(request.getHeader(HEADER_TENANT_ID)))
                        .clientIp(request.getHeader(HEADER_CLIENT_IP))
                        .clientType(request.getHeader(HEADER_CLIENT_TYPE))
                        .deviceId(request.getHeader(HEADER_DEVICE_ID))
                        .build();

                SecurityContextHolder.set(userDetails);
            }
        } catch (Exception e) {
            log.warn("Failed to extract user context from headers: {}", e.getMessage());
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clear();
        }
    }

    private Long parseLong(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}