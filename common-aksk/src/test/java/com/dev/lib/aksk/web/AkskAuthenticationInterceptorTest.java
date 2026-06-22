package com.dev.lib.aksk.web;

import com.dev.lib.aksk.annotation.Aksk;
import com.dev.lib.aksk.config.AkskProperties;
import com.dev.lib.aksk.domain.model.AkskAuthentication;
import com.dev.lib.aksk.domain.model.AkskVerificationRequest;
import com.dev.lib.aksk.domain.service.AkskHeaderResolver;
import com.dev.lib.aksk.domain.service.AkskSigner;
import com.dev.lib.aksk.domain.service.AkskVerifier;
import com.dev.lib.exceptions.BizException;
import com.dev.lib.security.util.SecurityContextHolder;
import com.dev.lib.web.model.StandardErrorCodes;
import jakarta.servlet.http.HttpServletRequest;
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

        SecurityContextHolder.clear();
    }

    @Test
    void endpointWithoutAkskShouldBeIgnored() throws Exception {

        RecordingVerifier verifier = new RecordingVerifier(authentication());
        RecordingSuccessHandler successHandler = new RecordingSuccessHandler();
        AkskAuthenticationInterceptor interceptor = interceptor(verifier, successHandler);

        boolean result = interceptor.preHandle(
                request("POST", "/partner/plain", "{}"),
                new MockHttpServletResponse(),
                handler(new PlainController(), "plain")
        );

        assertThat(result).isTrue();
        assertThat(verifier.calls).isZero();
        assertThat(successHandler.calls).isZero();
        assertThat(SecurityContextHolder.isLogin()).isFalse();
    }

    @Test
    void methodLevelAkskShouldBeDetected() throws Exception {

        RecordingVerifier verifier = new RecordingVerifier(authentication());
        AkskAuthenticationInterceptor interceptor = interceptor(verifier);
        HandlerMethod handler = handler(new PlainController(), "methodAksk");

        SecurityContextHolder.withEmptyContext(() -> interceptor.preHandle(
                request("POST", "/partner/method", "{}"),
                new MockHttpServletResponse(),
                handler
        ));

        assertThat(verifier.calls).isEqualTo(1);
        assertThat(verifier.lastRequest.getRequiredScopes()).containsExactly("method:scope");
        assertThat(verifier.lastRequest.getHeaders().accessKeyHeader()).isEqualTo("X-Method-Access-Key");
    }

    @Test
    void classLevelAkskShouldBeDetected() throws Exception {

        RecordingVerifier verifier = new RecordingVerifier(authentication());
        AkskAuthenticationInterceptor interceptor = interceptor(verifier);
        HandlerMethod handler = handler(new ClassLevelController(), "classAksk");

        SecurityContextHolder.withEmptyContext(() -> interceptor.preHandle(
                request("GET", "/partner/class", ""),
                new MockHttpServletResponse(),
                handler
        ));

        assertThat(verifier.calls).isEqualTo(1);
        assertThat(verifier.lastRequest.getRequiredScopes()).containsExactly("class:scope");
        assertThat(verifier.lastRequest.getHeaders().accessKeyHeader()).isEqualTo("X-Class-Access-Key");
    }

    @Test
    void methodLevelAkskShouldWinOverClassLevelAksk() throws Exception {

        RecordingVerifier verifier = new RecordingVerifier(authentication());
        AkskAuthenticationInterceptor interceptor = interceptor(verifier);
        HandlerMethod handler = handler(new ClassLevelController(), "methodOverrideAksk");

        SecurityContextHolder.withEmptyContext(() -> interceptor.preHandle(
                request("POST", "/partner/override", "raw-body"),
                new MockHttpServletResponse(),
                handler
        ));

        assertThat(verifier.calls).isEqualTo(1);
        assertThat(verifier.lastRequest.getRequiredScopes()).containsExactly("method:override");
        assertThat(verifier.lastRequest.getHeaders().accessKeyHeader()).isEqualTo("X-Method-Access-Key");
        assertThat(verifier.lastRequest.getMethod()).isEqualTo("POST");
        assertThat(verifier.lastRequest.getPath()).isEqualTo("/partner/override");
        assertThat(verifier.lastRequest.getBody()).isEqualTo("raw-body".getBytes());
    }

    @Test
    void validAkskRequestShouldSetContextAndNotifySuccessHandlers() throws Exception {

        AkskAuthentication authentication = authentication();
        RecordingVerifier verifier = new RecordingVerifier(authentication);
        RecordingSuccessHandler successHandler = new RecordingSuccessHandler();
        AkskAuthenticationInterceptor interceptor = interceptor(verifier, successHandler);
        MockHttpServletRequest request = request("POST", "/partner/method", "{}");
        HandlerMethod handler = handler(new PlainController(), "methodAksk");

        SecurityContextHolder.withEmptyContext(() -> {
            interceptor.preHandle(
                    request,
                    new MockHttpServletResponse(),
                    handler
            );
            assertThat(SecurityContextHolder.current().getUsername()).isEqualTo("partner-code");
//            assertThat(SecurityContextHolder.current().getRealName()).isEqualTo("Partner System");
            assertThat(SecurityContextHolder.getRoles()).containsExactly("AKSK");
            assertThat(SecurityContextHolder.getPermissions()).containsExactly("method:scope");
            assertThat(SecurityContextHolder.current().getClientIp()).isEqualTo("127.0.0.1");
//            assertThat(SecurityContextHolder.current().getExtra()).containsEntry("accessKey", "ak_partner");
        });
        assertThat(successHandler.calls).isEqualTo(1);
        assertThat(successHandler.authentication).isSameAs(authentication);
        assertThat(successHandler.request).isSameAs(request);
    }

    @Test
    void verificationFailureShouldNotWriteContextOrNotifySuccessHandlers() throws Exception {

        RecordingVerifier verifier = new RecordingVerifier(authentication());
        verifier.failure = new BizException(StandardErrorCodes.PERMISSION_DENIED, "AK/SK 授权范围不足");
        RecordingSuccessHandler successHandler = new RecordingSuccessHandler();
        AkskAuthenticationInterceptor interceptor = interceptor(verifier, successHandler);

        assertThatThrownBy(() -> interceptor.preHandle(
                request("POST", "/partner/method", "{}"),
                new MockHttpServletResponse(),
                handler(new PlainController(), "methodAksk")
        ))
                .isInstanceOf(BizException.class)
                .extracting("coder")
                .isEqualTo(StandardErrorCodes.PERMISSION_DENIED);

        assertThat(SecurityContextHolder.isLogin()).isFalse();
        assertThat(successHandler.calls).isZero();
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

    @Test
    void afterCompletionShouldLeaveSecurityContextToRequestScope() throws Exception {

        AkskAuthenticationInterceptor interceptor = interceptor(new RecordingVerifier(authentication()));
        MockHttpServletRequest request = request("POST", "/partner/method", "{}");
        HandlerMethod handler = handler(new PlainController(), "methodAksk");

        SecurityContextHolder.withEmptyContext(() -> {
            interceptor.preHandle(request, new MockHttpServletResponse(), handler);
            afterCompletion(interceptor, request, handler);

            assertThat(SecurityContextHolder.current().getUsername()).isEqualTo("partner-code");
        });

        assertThat(SecurityContextHolder.isLogin()).isFalse();
    }

    private AkskAuthenticationInterceptor interceptor(RecordingVerifier verifier) {

        return interceptor(verifier, new RecordingSuccessHandler());
    }

    private AkskAuthenticationInterceptor interceptor(
            RecordingVerifier verifier,
            AkskAuthenticationSuccessHandler successHandler
    ) {

        return new AkskAuthenticationInterceptor(
                verifier,
                new AkskHeaderResolver(),
                new AkskProperties(),
                List.of(successHandler)
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

    private void afterCompletion(
            AkskAuthenticationInterceptor interceptor,
            MockHttpServletRequest request,
            HandlerMethod handler
    ) {

        try {
            interceptor.afterCompletion(request, new MockHttpServletResponse(), handler, null);
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
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

    private static class RecordingSuccessHandler implements AkskAuthenticationSuccessHandler {

        private int calls;

        private AkskAuthentication authentication;

        private HttpServletRequest request;

        @Override
        public void onSuccess(AkskAuthentication authentication, HttpServletRequest request) {

            calls++;
            this.authentication = authentication;
            this.request = request;
        }
    }
}
