package com.dev.lib.aksk.web;

import com.dev.lib.aksk.config.AkskProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@RequiredArgsConstructor
public class AkskRequestBodyFilter extends OncePerRequestFilter {

    public static final String BODY_CACHE_SKIPPED_ATTRIBUTE =
            AkskRequestBodyFilter.class.getName() + ".BODY_CACHE_SKIPPED";

    private static final Set<String> BODY_METHODS = Set.of("POST", "PUT", "PATCH");

    private final AkskProperties properties;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        if (!shouldWrap(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        byte[] body = request.getInputStream().readAllBytes();
        filterChain.doFilter(new CachedBodyHttpServletRequest(request, body), response);
    }

    private boolean shouldWrap(HttpServletRequest request) {

        if (request instanceof CachedBodyHttpServletRequest) {
            return false;
        }
        if (!BODY_METHODS.contains(request.getMethod())) {
            return false;
        }
        long contentLength = request.getContentLengthLong();
        int maxCachedBodyBytes = properties == null ? new AkskProperties().getMaxCachedBodyBytes() : properties.getMaxCachedBodyBytes();
        if (contentLength > maxCachedBodyBytes) {
            request.setAttribute(BODY_CACHE_SKIPPED_ATTRIBUTE, true);
            return false;
        }
        return contentLength <= maxCachedBodyBytes;
    }
}
