package com.dev.lib.web;

import com.dev.lib.entity.id.IDWorker;
import com.dev.lib.entity.id.IntEncoder;
import com.dev.lib.security.util.ClientInfoExtractor;
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
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static net.logstash.logback.argument.StructuredArguments.keyValue;

@Slf4j
@Order(3)
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

    private void logRequest(ContentCachingRequestWrapper request) throws UnsupportedEncodingException {
        Map<String, Object> requestInfo = new HashMap<>();
        requestInfo.put("method", request.getMethod());
        requestInfo.put("path", request.getRequestURI());
        requestInfo.put("source_ip", ClientInfoExtractor.getClientIp(request));

        List<StructuredArgument> args = new ArrayList<>(List.of(
                keyValue("context", requestInfo)
        ));

        if (SecurityContextHolder.isLogin()) {
            UserDetails user = SecurityContextHolder.get();
            Map<String, Object> userInfo = Map.of(
                    "id", Optional.ofNullable(user.getId()).map(Objects::toString).orElse("/"),
                    "username", Optional.ofNullable(user.getUsername()).orElse("Anonymous")
            );
            args.add(keyValue("user", userInfo));
        }

        Map<String, Object> business = new HashMap<>();
        boolean hasBusiness = false;

        if ("POST".equals(request.getMethod()) || "PUT".equals(request.getMethod())) {
            byte[] content = request.getContentAsByteArray();
            if (content.length > 0) {
                try {
                    String body = new String(content, request.getCharacterEncoding());
                    business.put("request_body", objectMapper.readValue(body, Object.class));
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