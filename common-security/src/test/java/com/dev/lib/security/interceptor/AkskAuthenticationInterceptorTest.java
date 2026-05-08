package com.dev.lib.security.interceptor;

import com.dev.lib.aksk.annotation.Aksk;
import com.dev.lib.aksk.config.AkskProperties;
import com.dev.lib.aksk.domain.model.AkskAuthentication;
import com.dev.lib.aksk.domain.model.AkskContextHolder;
import com.dev.lib.aksk.domain.model.AkskVerificationRequest;
import com.dev.lib.aksk.domain.service.AkskHeaderResolver;
import com.dev.lib.aksk.domain.service.AkskSigner;
import com.dev.lib.aksk.domain.service.AkskVerifier;
import com.dev.lib.exceptions.BizException;
import com.dev.lib.security.aksk.AkskRequestBodyFilter;
import com.dev.lib.security.aksk.AkskSecurityContextAdapter;
import com.dev.lib.security.util.SecurityContextHolder;
import com.dev.lib.web.model.StandardErrorCodes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AkskAuthenticationInterceptorTest {

    @AfterEach
    void tearDown() {

        AkskContextHolder.clear();
        SecurityContextHolder.clear();
    }

    @Test
    void endpointWithoutAkskShouldBeIgnored() throws Exception {

        RecordingVerifier verifier = new RecordingVerifier(authentication());
        AkskAuthenticationInterceptor interceptor = interceptor(verifier);

        boolean result = interceptor.preHandle(
                request("POST", "/partner/plain", "{}"),
                new MockHttpServletResponse(),
                handler(new PlainController(), "plain")
        );

        assertThat(result).isTrue();
        assertThat(verifier.calls).isZero();
        assertThat(AkskContextHolder.isAuthenticated()).isFalse();
        assertThat(SecurityContextHolder.isLogin()).isFalse();
    }

    @Test
    void methodLevelAkskShouldBeDetected() throws Exception {

        RecordingVerifier verifier = new RecordingVerifier(authentication());
        AkskAuthenticationInterceptor interceptor = interceptor(verifier);

        interceptor.preHandle(
                request("POST", "/partner/method", "{}"),
                new MockHttpServletResponse(),
                handler(new PlainController(), "methodAksk")
        );

        assertThat(verifier.calls).isEqualTo(1);
        assertThat(verifier.lastRequest.getRequiredScopes()).containsExactly("method:scope");
        assertThat(verifier.lastRequest.getHeaders().accessKeyHeader()).isEqualTo("X-Method-Access-Key");
    }

    @Test
    void classLevelAkskShouldBeDetected() throws Exception {

        RecordingVerifier verifier = new RecordingVerifier(authentication());
        AkskAuthenticationInterceptor interceptor = interceptor(verifier);

        interceptor.preHandle(
                request("GET", "/partner/class", ""),
                new MockHttpServletResponse(),
                handler(new ClassLevelController(), "classAksk")
        );

        assertThat(verifier.calls).isEqualTo(1);
        assertThat(verifier.lastRequest.getRequiredScopes()).containsExactly("class:scope");
        assertThat(verifier.lastRequest.getHeaders().accessKeyHeader()).isEqualTo("X-Class-Access-Key");
    }

    @Test
    void methodLevelAkskShouldWinOverClassLevelAksk() throws Exception {

        RecordingVerifier verifier = new RecordingVerifier(authentication());
        AkskAuthenticationInterceptor interceptor = interceptor(verifier);

        interceptor.preHandle(
                request("POST", "/partner/override", "raw-body"),
                new MockHttpServletResponse(),
                handler(new ClassLevelController(), "methodOverrideAksk")
        );

        assertThat(verifier.calls).isEqualTo(1);
        assertThat(verifier.lastRequest.getRequiredScopes()).containsExactly("method:override");
        assertThat(verifier.lastRequest.getHeaders().accessKeyHeader()).isEqualTo("X-Method-Access-Key");
        assertThat(verifier.lastRequest.getMethod()).isEqualTo("POST");
        assertThat(verifier.lastRequest.getPath()).isEqualTo("/partner/override");
        assertThat(verifier.lastRequest.getBody()).isEqualTo("raw-body".getBytes());
    }

    @Test
    void validAkskRequestShouldWriteBothSecurityContexts() throws Exception {

        RecordingVerifier verifier = new RecordingVerifier(authentication());
        AkskAuthenticationInterceptor interceptor = interceptor(verifier);

        interceptor.preHandle(
                request("POST", "/partner/method", "{}"),
                new MockHttpServletResponse(),
                handler(new PlainController(), "methodAksk")
        );

        assertThat(AkskContextHolder.current().getAccessKey()).isEqualTo("ak_partner");
        assertThat(SecurityContextHolder.current().getUsername()).isEqualTo("partner-code");
        assertThat(SecurityContextHolder.current().getRealName()).isEqualTo("Partner System");
        assertThat(SecurityContextHolder.current().getValidated()).isTrue();
        assertThat(SecurityContextHolder.getRoles()).contains("AKSK");
        assertThat(SecurityContextHolder.getRoles()).doesNotContain("admin");
        assertThat(SecurityContextHolder.getPermissions()).containsExactly("method:scope");
    }

    @Test
    void scopeMismatchShouldFailBeforeBusinessMethod() throws Exception {

        RecordingVerifier verifier = new RecordingVerifier(authentication());
        verifier.failure = new BizException(StandardErrorCodes.PERMISSION_DENIED, "AK/SK 授权范围不足");
        AkskAuthenticationInterceptor interceptor = interceptor(verifier);

        assertThatThrownBy(() -> interceptor.preHandle(
                request("POST", "/partner/method", "{}"),
                new MockHttpServletResponse(),
                handler(new PlainController(), "methodAksk")
        ))
                .isInstanceOf(BizException.class)
                .extracting("coder")
                .isEqualTo(StandardErrorCodes.PERMISSION_DENIED);

        assertThat(AkskContextHolder.isAuthenticated()).isFalse();
        assertThat(SecurityContextHolder.isLogin()).isFalse();
    }

    @Test
    void oversizedBodySkippedByFilterShouldFailWithoutCallingVerifier() throws Exception {

        RecordingVerifier verifier = new RecordingVerifier(authentication());
        AkskAuthenticationInterceptor interceptor = interceptor(verifier);
        MockHttpServletRequest request = request("POST", "/partner/method", "oversized-body");
        request.setAttribute(AkskRequestBodyFilter.BODY_CACHE_SKIPPED_ATTRIBUTE, true);

        assertThatThrownBy(() -> interceptor.preHandle(
                request,
                new MockHttpServletResponse(),
                handler(new PlainController(), "methodAksk")
        ))
                .isInstanceOf(BizException.class)
                .extracting("coder")
                .isEqualTo(StandardErrorCodes.REQUEST_BODY_INVALID);

        assertThat(verifier.calls).isZero();
        assertThat(request.getInputStream().readAllBytes()).isEqualTo("oversized-body".getBytes());
    }

    private AkskAuthenticationInterceptor interceptor(RecordingVerifier verifier) {

        return new AkskAuthenticationInterceptor(
                verifier,
                new AkskHeaderResolver(),
                new AkskProperties(),
                new AkskSecurityContextAdapter()
        );
    }

    private MockHttpServletRequest request(String method, String uri, String body) {

        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setRequestURI(uri);
        request.setContent(body.getBytes());
        request.setRemoteAddr("127.0.0.1");
        return request;
    }

    private HandlerMethod handler(Object bean, String methodName) throws NoSuchMethodException {

        Method method = bean.getClass().getMethod(methodName);
        return new HandlerMethod(bean, method);
    }

    private AkskAuthentication authentication() {

        AkskAuthentication authentication = new AkskAuthentication();
        authentication.setCredentialId("cred-1");
        authentication.setAccessKey("ak_partner");
        authentication.setSubjectName("Partner System");
        authentication.setSubjectCode("partner-code");
        authentication.setScopes(new LinkedHashSet<>(Set.of("method:scope")));
        authentication.setProperties(Map.of("tenant", "blue"));
        authentication.setClientIp("127.0.0.1");
        authentication.setAuthenticatedAt(LocalDateTime.now());
        return authentication;
    }

    static class PlainController {

        public void plain() {

        }

        @Aksk(scopes = "method:scope", headerPrefix = "X-Method")
        public void methodAksk() {

        }
    }

    @Aksk(scopes = "class:scope", headerPrefix = "X-Class")
    static class ClassLevelController {

        public void classAksk() {

        }

        @Aksk(scopes = "method:override", headerPrefix = "X-Method")
        public void methodOverrideAksk() {

        }
    }

    private static class RecordingVerifier extends AkskVerifier {

        private int calls;

        private AkskVerificationRequest lastRequest;

        private BizException failure;

        private final AkskAuthentication authentication;

        private RecordingVerifier(AkskAuthentication authentication) {

            super(null, new AkskSigner(), new AkskProperties());
            this.authentication = authentication;
        }

        @Override
        public AkskAuthentication verify(AkskVerificationRequest request) {

            calls++;
            lastRequest = request;
            if (failure != null) {
                throw failure;
            }
            return authentication;
        }
    }
}
