package com.dev.lib.log;

import com.dev.lib.entity.id.IDWorker;
import com.dev.lib.entity.id.IntEncoder;
import com.dev.lib.security.util.SecurityContextHolder;
import com.dev.lib.security.util.UserDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArgument;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static net.logstash.logback.argument.StructuredArguments.keyValue;

@Slf4j
@Order(2)
@Component
@RequiredArgsConstructor
public class LoggingFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        HttpServletRequest httpRequest = request;
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(httpRequest);

        try {
            // 1. trace_id 用 MDC
            String traceId = httpRequest.getHeader("X-Trace-Id");
            if (traceId == null) {
                traceId = IntEncoder.encode52(IDWorker.nextID());
            }
            MDC.put("trace_id", traceId);

            // 2. 构建 context 对象
            Map<String, Object> requestInfo = new HashMap<>();
            requestInfo.put("method", httpRequest.getMethod());
            requestInfo.put("path", httpRequest.getRequestURI());
            requestInfo.put("source_ip", getClientIp(httpRequest));
            List<StructuredArgument> args =
                    new ArrayList<>(List.of(keyValue("context", requestInfo)));


            // 添加用户信息
            if (SecurityContextHolder.isLogin()) {
                UserDetails user = SecurityContextHolder.get();
                Map<String, Object> userInfo = Map.of(
                        "id", Optional.ofNullable(user.getId()).map(Objects::toString).orElse("/"),
                        "username", Optional.ofNullable(user.getUsername()).orElse("Anonymous")
                );
                args.add(keyValue("user", userInfo));
            }

            // 3. 构建 business 对象（body 或 query params）
            Map<String, Object> business = new HashMap<>();
            boolean hasBusiness = false;

            // 3.1 处理 request body（POST/PUT）
            if ("POST".equals(httpRequest.getMethod()) || "PUT".equals(httpRequest.getMethod())) {
                byte[] content = wrappedRequest.getContentAsByteArray();
                if (content.length > 0) {
                    try {
                        String body = new String(content);
                        business.put("request_body", objectMapper.readValue(body, Object.class));
                        hasBusiness = true;
                    } catch (Exception e) {
                        // 非 JSON body 就直接放字符串
                        business.put("request_body", new String(content));
                        hasBusiness = true;
                    }
                }
            }

            // 3.2 处理 query params（所有方法都收集）
            String queryString = httpRequest.getQueryString();
            if (queryString != null && !queryString.isEmpty()) {
                Map<String, Object> params = new HashMap<>();
                httpRequest.getParameterMap().forEach((key, values) -> {
                    if (values.length == 1) {
                        params.put(key, values[0]);
                    } else {
                        params.put(key, Arrays.asList(values));
                    }
                });
                business.put("query_params", params);
                hasBusiness = true;
            }

            // 4. 用 StructuredArguments 记录
            if (hasBusiness) {
                args.add(keyValue("business", business));
            }

            log.info("Request received", args.toArray());
            filterChain.doFilter(wrappedRequest, response);
        } finally {
            MDC.clear();
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip.split(",")[0].trim();
    }
}