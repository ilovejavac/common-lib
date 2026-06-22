package com.dev.lib.aksk.web;

import com.dev.lib.aksk.annotation.Aksk;
import com.dev.lib.aksk.config.AkskProperties;
import com.dev.lib.aksk.domain.model.AkskAuthentication;
import com.dev.lib.aksk.domain.model.AkskVerificationRequest;
import com.dev.lib.aksk.domain.service.AkskHeaderResolver;
import com.dev.lib.aksk.domain.service.AkskVerifier;
import com.dev.lib.exceptions.BizException;
import com.dev.lib.security.util.ClientInfoExtractor;
import com.dev.lib.security.util.SecurityContextHolder;
import com.dev.lib.security.util.UserDetails;
import com.dev.lib.web.model.StandardErrorCodes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RequiredArgsConstructor
public class AkskAuthenticationInterceptor implements HandlerInterceptor {

    private static final String AKSK_ROLE = "AKSK";

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
        SecurityContextHolder.set(toUserDetails(authentication));
        successHandlers.forEach(successHandler -> successHandler.onSuccess(authentication, request));
        return true;
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

    private UserDetails toUserDetails(AkskAuthentication authentication) {

        return UserDetails.builder()
                .id(syntheticUserId(authentication))
                .username(username(authentication))
                .roles(List.of(AKSK_ROLE))
                .permissions(new ArrayList<>(Optional.ofNullable(authentication.getScopes()).orElse(Set.of())))
                .validated(true)
                .clientIp(authentication.getClientIp())
                .build();
    }

    private Long syntheticUserId(AkskAuthentication authentication) {

        int hash = authentication.getCredentialId() == null
                   ? Optional.ofNullable(authentication.getAccessKey()).orElse("aksk").hashCode()
                   : authentication.getCredentialId().hashCode();
        long unsignedHash = Integer.toUnsignedLong(hash);
        return -Math.max(1L, unsignedHash);
    }

    private String username(AkskAuthentication authentication) {

        if (StringUtils.hasText(authentication.getSubjectCode())) {
            return authentication.getSubjectCode();
        }
        if (StringUtils.hasText(authentication.getAccessKey())) {
            return authentication.getAccessKey();
        }
        return "aksk";
    }

    private Map<String, Object> extra(AkskAuthentication authentication) {

        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("akskCredentialId", authentication.getCredentialId());
        extra.put("accessKey", authentication.getAccessKey());
        extra.put("subjectCode", authentication.getSubjectCode());
        extra.put("properties", authentication.getProperties());
        return extra;
    }
}
