package com.dev.lib.web;

import com.alibaba.fastjson2.JSON;
import com.dev.lib.entity.id.IDWorker;
import com.dev.lib.entity.id.IntEncoder;
import com.dev.lib.security.util.ClientInfoExtractor;
import com.dev.lib.security.util.SecurityContextHolder;
import com.dev.lib.security.util.UserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArgument;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;

import static net.logstash.logback.argument.StructuredArguments.keyValue;

@Slf4j
@Order(3)
@Component
@RequiredArgsConstructor
public class LoggingFilter extends OncePerRequestFilter {

    private static final int CACHE_LIMIT = 64 * 1024; // 10KB

    private static final String[] LOG_PATTERNS = {"/api/**"};

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {

        HttpServletRequest           httpRequest    = request;
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(httpRequest, CACHE_LIMIT);
        String                       requestPath    = request.getRequestURI();

        // ✅ 只处理 /api/** 路径
        if (!shouldLog(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            String traceId = request.getHeader("X-Trace-Id");
            if (traceId == null) {
                traceId = IntEncoder.encode52(IDWorker.nextID());
            }
            MDC.put("trace_id", traceId);

            // ✅ 先放行，让 Controller 消费流
            filterChain.doFilter(wrappedRequest, response);
        } finally {
            logRequest(wrappedRequest);
            MDC.clear();
        }
    }

    /**
     * 判断是否需要记录日志
     */
    private boolean shouldLog(String path) {

        for (String pattern : LOG_PATTERNS) {
            if (matchesPattern(path, pattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 简单的路径匹配（支持 ** 通配符）
     */
    private boolean matchesPattern(String path, String pattern) {

        if (pattern.endsWith("/**")) {
            String prefix = pattern.substring(0, pattern.length() - 3);
            return path.startsWith(prefix);
        }
        return path.equals(pattern);
    }

    private void logRequest(ContentCachingRequestWrapper request) throws UnsupportedEncodingException {

        Map<String, Object> requestInfo = new HashMap<>();
        requestInfo.put("method", request.getMethod());
        requestInfo.put("path", request.getRequestURI());
        requestInfo.put("source_ip", ClientInfoExtractor.getClientIp(request));

        List<StructuredArgument> args = new ArrayList<>(List.of(keyValue("context", requestInfo)));

        if (SecurityContextHolder.isLogin()) {
            UserDetails user = SecurityContextHolder.get();
            Map<String, Object> userInfo = Map.of(
                    "id",
                    Optional.ofNullable(user.getId()).map(Objects::toString).orElse("/"),
                    "username",
                    Optional.ofNullable(user.getUsername()).orElse("Anonymous")
            );
            args.add(keyValue("user", userInfo));
        }

        Map<String, Object> business    = new HashMap<>();
        boolean             hasBusiness = false;

        if ("POST".equals(request.getMethod()) || "PUT".equals(request.getMethod())) {
            byte[] content = request.getContentAsByteArray();
            if (content.length > 0) {
                try {
                    String body = new String(content, request.getCharacterEncoding());
                    business.put("request_body", JSON.parse(body));
                    hasBusiness = true;
                } catch (Exception e) {
                    business.put("request_body", new String(content, request.getCharacterEncoding()));
                    hasBusiness = true;
                }
            }
        }

        String queryString = request.getQueryString();
        if (queryString != null && !queryString.isEmpty()) {
            Map<String, Object> params = new HashMap<>();
            request.getParameterMap().forEach((key, values) -> {
                params.put(key, values.length == 1 ? values[0] : Arrays.asList(values));
            });
            business.put("query_params", params);
            hasBusiness = true;
        }

        if (hasBusiness) {
            args.add(keyValue("business", business));
        }

        log.info("Request received", args.toArray());
    }

}