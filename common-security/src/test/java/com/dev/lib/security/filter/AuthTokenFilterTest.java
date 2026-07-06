package com.dev.lib.security.filter;

import com.dev.lib.security.TokenException;
import com.dev.lib.security.model.UserStatus;
import com.dev.lib.security.service.TokenService;
import com.dev.lib.security.web.UserContextHeaders;
import com.dev.lib.security.util.SecurityContextHolder;
import com.dev.lib.security.util.UserDetails;
import com.dev.lib.util.ImmutablePair;
import com.dev.lib.web.model.StandardErrorCodes;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AuthTokenFilterTest {

    @AfterEach
    void tearDown() {

        SecurityContextHolder.clear();
    }

    @Test
    void bearerTokenShouldPopulateSecurityContextForDownstreamChain() throws Exception {

        RecordingTokenService tokenService = new RecordingTokenService();
        AuthTokenFilter filter = new AuthTokenFilter(tokenService);
        MockHttpServletRequest request = request();
        request.addHeader("Authorization", "Bearer token-1");
        request.addHeader("X-Forwarded-For", "10.0.0.1, 10.0.0.2");
        request.addHeader("X-Client-Type", "web");
        request.addHeader("X-Device-Id", "device-1");
        CapturingChain chain = new CapturingChain(() -> {
            assertThat(SecurityContextHolder.current().getUsername()).isEqualTo("demo");
            assertThat(SecurityContextHolder.current().getClientIp()).isEqualTo("10.0.0.1");
            assertThat(SecurityContextHolder.current().getClientType()).isEqualTo("WEB");
            assertThat(SecurityContextHolder.current().getDeviceId()).isEqualTo("device-1");
        });

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(chain.called).isTrue();
        assertThat(tokenService.parsedToken).isEqualTo("token-1");
        assertThat(SecurityContextHolder.isLogin()).isFalse();
    }

    @Test
    void nonBearerAuthorizationShouldNotParseToken() throws Exception {

        RecordingTokenService tokenService = new RecordingTokenService();
        AuthTokenFilter filter = new AuthTokenFilter(tokenService);
        MockHttpServletRequest request = request();
        request.addHeader("Authorization", "Token token-1");
        CapturingChain chain = new CapturingChain(() ->
                assertThat(SecurityContextHolder.isLogin()).isFalse()
        );

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(chain.called).isTrue();
        assertThat(tokenService.parsedToken).isNull();
    }

    @Test
    void gatewayUserHeadersShouldPopulateSecurityContextWithoutParsingToken() throws Exception {

        RecordingTokenService tokenService = new RecordingTokenService();
        AuthTokenFilter filter = new AuthTokenFilter(tokenService);
        MockHttpServletRequest request = request();
        request.addHeader("Authorization", "Bearer token-should-not-be-parsed");
        request.addHeader(UserContextHeaders.USER_VALIDATED, "true");
        request.addHeader(UserContextHeaders.USER_ID, "7");
        request.addHeader(UserContextHeaders.USER_NAME, "alice");
        request.addHeader(UserContextHeaders.USER_ROLE, "admin,user");
        request.addHeader(UserContextHeaders.USER_PERMISSION, "order:create,order:read");
        request.addHeader(UserContextHeaders.USER_TENANT, "88");
        request.addHeader(UserContextHeaders.USER_DEPT_ID, "99");
        request.addHeader(UserContextHeaders.USER_STATUS, "ACTIVE");
        request.addHeader(UserContextHeaders.USER_CLIENT_IP, "10.0.0.1");
        request.addHeader(UserContextHeaders.USER_CLIENT_TYPE, "WEB");
        request.addHeader(UserContextHeaders.USER_DEVICE_ID, "device-1");
        CapturingChain chain = new CapturingChain(() -> {
            UserDetails current = SecurityContextHolder.current();
            assertThat(current.getValidated()).isTrue();
            assertThat(current.getId()).isEqualTo(7L);
            assertThat(current.getUsername()).isEqualTo("alice");
            assertThat(current.getRoles()).containsExactly("admin", "user");
            assertThat(current.getPermissions()).containsExactly("order:create", "order:read");
            assertThat(current.getTenant()).isEqualTo(88L);
            assertThat(current.getDeptId()).isEqualTo(99L);
            assertThat(current.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(current.getClientIp()).isEqualTo("10.0.0.1");
            assertThat(current.getClientType()).isEqualTo("WEB");
            assertThat(current.getDeviceId()).isEqualTo("device-1");
        });

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(chain.called).isTrue();
        assertThat(tokenService.parsedToken).isNull();
    }

    @Test
    void invalidBearerTokenShouldWriteFailureResponseWithoutCallingChain() throws Exception {

        AuthTokenFilter filter = new AuthTokenFilter(new InvalidTokenService());
        MockHttpServletRequest request = request();
        request.addHeader("Authorization", "Bearer expired-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        CapturingChain chain = new CapturingChain(() -> {
        });

        filter.doFilter(request, response, chain);

        assertThat(chain.called).isFalse();
        assertThat(response.getContentType()).isEqualTo("application/json;charset=UTF-8");
        assertThat(response.getContentAsString()).contains("\"code\":" + StandardErrorCodes.TOKEN_INVALID);
        assertThat(response.getContentAsString()).contains("\"message\":\"response failed\"");
        assertThat(response.getContentAsString()).contains("\"error\":\"token已过期\"");
    }

    private MockHttpServletRequest request() {

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/demo");
        request.setRequestURI("/api/demo");
        request.setRemoteAddr("127.0.0.1");
        return request;
    }

    private static class RecordingTokenService implements TokenService {

        private String parsedToken;

        @Override
        public ImmutablePair<String, String> generateToken(UserDetails userDetails) {

            return ImmutablePair.of("token", "refresh-token");
        }

        @Override
        public UserDetails parseToken(String token) {

            parsedToken = token;
            return UserDetails.builder()
                    .id(1L)
                    .username("demo")
                    .roles(List.of("user"))
                    .validated(true)
                    .build();
        }
    }

    private static class InvalidTokenService implements TokenService {

        @Override
        public ImmutablePair<String, String> generateToken(UserDetails userDetails) {

            return ImmutablePair.of("token", "refresh-token");
        }

        @Override
        public UserDetails parseToken(String token) {

            throw new TokenException("token已过期");
        }
    }

    private static class CapturingChain implements FilterChain {

        private final CheckedRunnable assertion;

        private boolean called;

        private CapturingChain(CheckedRunnable assertion) {

            this.assertion = assertion;
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {

            called = true;
            assertion.run();
        }
    }

    @FunctionalInterface
    private interface CheckedRunnable {

        void run() throws IOException, ServletException;
    }
}
