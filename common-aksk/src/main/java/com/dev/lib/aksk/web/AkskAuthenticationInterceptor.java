package com.dev.lib.aksk.web;

import com.dev.lib.aksk.annotation.Aksk;
import com.dev.lib.aksk.config.AkskProperties;
import com.dev.lib.aksk.domain.model.AkskAuthentication;
import com.dev.lib.aksk.domain.model.AkskContextHolder;
import com.dev.lib.aksk.domain.model.AkskVerificationRequest;
import com.dev.lib.aksk.domain.service.AkskHeaderResolver;
import com.dev.lib.aksk.domain.service.AkskVerifier;
import com.dev.lib.exceptions.BizException;
import com.dev.lib.security.util.ClientInfoExtractor;
import com.dev.lib.web.model.StandardErrorCodes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

@RequiredArgsConstructor
public class AkskAuthenticationInterceptor implements HandlerInterceptor {

    private final AkskVerifier verifier;

    private final AkskHeaderResolver headerResolver;

    private final AkskProperties properties;

    private final List<AkskAuthenticationSuccessHandler> successHandlers;

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler
    ) {

        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        Aksk annotation = resolveAnnotation(handlerMethod);
        if (annotation == null) {
            return true;
        }

        AkskAuthentication authentication = verifier.verify(verificationRequest(request, annotation));
        AkskContextHolder.set(authentication);
        successHandlers.forEach(successHandler -> successHandler.onSuccess(authentication, request));
        return true;
    }

    @Override
    public void afterCompletion(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler,
            Exception ex
    ) {

        AkskContextHolder.clear();
    }

    private Aksk resolveAnnotation(HandlerMethod handlerMethod) {

        Aksk methodAnnotation = handlerMethod.getMethodAnnotation(Aksk.class);
        if (methodAnnotation != null) {
            return methodAnnotation;
        }
        return handlerMethod.getBeanType().getAnnotation(Aksk.class);
    }

    private AkskVerificationRequest verificationRequest(HttpServletRequest request, Aksk annotation) {

        AkskVerificationRequest verificationRequest = new AkskVerificationRequest();
        verificationRequest.setMethod(request.getMethod());
        verificationRequest.setPath(request.getRequestURI());
        verificationRequest.setBody(body(request));
        verificationRequest.setClientIp(ClientInfoExtractor.getClientIp(request));
        verificationRequest.setHeaders(headerResolver.resolve(annotation, properties));
        verificationRequest.setHeaderLookup(request::getHeader);
        verificationRequest.setRequiredScopes(new LinkedHashSet<>(Arrays.asList(annotation.scopes())));
        return verificationRequest;
    }

    private byte[] body(HttpServletRequest request) {

        if (Boolean.TRUE.equals(request.getAttribute(AkskRequestBodyFilter.BODY_CACHE_SKIPPED_ATTRIBUTE))) {
            throw new BizException(StandardErrorCodes.REQUEST_BODY_INVALID, "AK/SK 请求体超过缓存限制，无法验签");
        }
        if (request instanceof CachedBodyHttpServletRequest cachedRequest) {
            return cachedRequest.getCachedBody();
        }
        try {
            return request.getInputStream().readAllBytes();
        } catch (IOException ex) {
            throw new BizException(StandardErrorCodes.REQUEST_BODY_INVALID, "AK/SK 请求体读取失败");
        }
    }
}
