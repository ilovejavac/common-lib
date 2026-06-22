package com.dev.lib.cloud;

import com.dev.lib.security.filter.AuthTokenFilter;
import com.dev.lib.security.model.UserStatus;
import com.dev.lib.security.util.SecurityContextHolder;
import com.dev.lib.security.util.UserDetails;
import com.dev.lib.security.web.UserContextHeaders;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UserContextRelayFilterTest {

    @AfterEach
    void tearDown() {

        SecurityContextHolder.clear();
    }

    @Test
    void shouldExposeAuthenticatedUserHeadersAfterAuthentication() throws Exception {

        UserContextRelayFilter filter = new UserContextRelayFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/demo");
        CapturingChain chain = new CapturingChain(servletRequest -> {
            HttpServletRequest wrapped = (HttpServletRequest) servletRequest;
            assertThat(wrapped.getHeader(UserContextHeaders.USER_VALIDATED)).isEqualTo("true");
            assertThat(wrapped.getHeader(UserContextHeaders.USER_ID)).isEqualTo("7");
            assertThat(wrapped.getHeader(UserContextHeaders.USER_NAME)).isEqualTo("alice");
            assertThat(wrapped.getHeader(UserContextHeaders.USER_ROLE)).isEqualTo("admin,user");
            assertThat(wrapped.getHeader(UserContextHeaders.USER_PERMISSION)).isEqualTo("order:create,order:read");
            assertThat(wrapped.getHeader(UserContextHeaders.USER_TENANT)).isEqualTo("88");
            assertThat(wrapped.getHeader(UserContextHeaders.USER_DEPT_ID)).isEqualTo("99");
            assertThat(wrapped.getHeader(UserContextHeaders.USER_STATUS)).isEqualTo("ACTIVE");
            assertThat(wrapped.getHeader(UserContextHeaders.USER_CLIENT_IP)).isEqualTo("10.0.0.1");
            assertThat(wrapped.getHeader(UserContextHeaders.USER_CLIENT_TYPE)).isEqualTo("WEB");
            assertThat(wrapped.getHeader(UserContextHeaders.USER_DEVICE_ID)).isEqualTo("device-1");
        });

        SecurityContextHolder.with(user(), () -> {
            try {
                filter.doFilter(request, new MockHttpServletResponse(), chain);
            } catch (IOException | ServletException ex) {
                throw new AssertionError(ex);
            }
        });

        assertThat(chain.called).isTrue();
    }

    @Test
    void shouldRunAfterTokenFilterSoUserHeadersAreAddedAfterTokenParsing() {

        assertThat(new UserContextRelayFilter().getOrder()).isGreaterThan(AuthTokenFilter.AUTH_TOKEN_FILTER_ORDER);
    }

    private UserDetails user() {

        return UserDetails.builder()
                .validated(true)
                .id(7L)
                .username("alice")
                .roles(List.of("admin", "user"))
                .permissions(List.of("order:create", "order:read"))
                .tenant(88L)
                .deptId(99L)
                .status(UserStatus.ACTIVE)
                .clientIp("10.0.0.1")
                .clientType("WEB")
                .deviceId("device-1")
                .build();
    }

    private static class CapturingChain implements FilterChain {

        private final CheckedConsumer assertion;

        private boolean called;

        private CapturingChain(CheckedConsumer assertion) {

            this.assertion = assertion;
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {

            called = true;
            assertion.accept(request);
        }
    }

    @FunctionalInterface
    private interface CheckedConsumer {

        void accept(ServletRequest request) throws IOException, ServletException;
    }
}
