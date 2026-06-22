package com.dev.lib.cloud;

import com.dev.lib.security.filter.AuthTokenFilter;
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

class AuthGlobalFilterTest {

    @AfterEach
    void tearDown() {

        SecurityContextHolder.clear();
    }

    @Test
    void shouldRemoveIncomingUserHeadersBeforeAuthentication() throws Exception {

        AuthGlobalFilter filter = new AuthGlobalFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/demo");
        request.addHeader(UserContextHeaders.USER_VALIDATED, "false");
        request.addHeader(UserContextHeaders.USER_ID, "forged-user");
        request.addHeader(UserContextHeaders.USER_NAME, "forged-name");
        request.addHeader(UserContextHeaders.USER_ROLE, "forged-role");
        request.addHeader(UserContextHeaders.USER_PERMISSION, "forged-permission");
        request.addHeader(UserContextHeaders.USER_TENANT, "forged-tenant");
        request.addHeader(UserContextHeaders.USER_DEPT_ID, "forged-dept");
        request.addHeader(UserContextHeaders.USER_STATUS, "DISABLED");
        request.addHeader(UserContextHeaders.USER_CLIENT_IP, "forged-ip");
        request.addHeader(UserContextHeaders.USER_CLIENT_TYPE, "forged-client");
        request.addHeader(UserContextHeaders.USER_DEVICE_ID, "forged-device");
        CapturingChain chain = new CapturingChain(servletRequest -> {
            HttpServletRequest wrapped = (HttpServletRequest) servletRequest;
            assertThat(wrapped.getHeader(UserContextHeaders.USER_VALIDATED)).isNull();
            assertThat(wrapped.getHeader(UserContextHeaders.USER_ID)).isNull();
            assertThat(wrapped.getHeader(UserContextHeaders.USER_NAME)).isNull();
            assertThat(wrapped.getHeader(UserContextHeaders.USER_ROLE)).isNull();
            assertThat(wrapped.getHeader(UserContextHeaders.USER_PERMISSION)).isNull();
            assertThat(wrapped.getHeader(UserContextHeaders.USER_TENANT)).isNull();
            assertThat(wrapped.getHeader(UserContextHeaders.USER_DEPT_ID)).isNull();
            assertThat(wrapped.getHeader(UserContextHeaders.USER_STATUS)).isNull();
            assertThat(wrapped.getHeader(UserContextHeaders.USER_CLIENT_IP)).isNull();
            assertThat(wrapped.getHeader(UserContextHeaders.USER_CLIENT_TYPE)).isNull();
            assertThat(wrapped.getHeader(UserContextHeaders.USER_DEVICE_ID)).isNull();

            SecurityContextHolder.with(user(), () -> {
                assertThat(wrapped.getHeader(UserContextHeaders.USER_VALIDATED)).isNull();
                assertThat(wrapped.getHeader(UserContextHeaders.USER_ID)).isNull();
                assertThat(wrapped.getHeader(UserContextHeaders.USER_NAME)).isNull();
                assertThat(wrapped.getHeader(UserContextHeaders.USER_ROLE)).isNull();
                assertThat(wrapped.getHeader(UserContextHeaders.USER_PERMISSION)).isNull();
                assertThat(wrapped.getHeader(UserContextHeaders.USER_TENANT)).isNull();
                assertThat(wrapped.getHeader(UserContextHeaders.USER_DEPT_ID)).isNull();
                assertThat(wrapped.getHeader(UserContextHeaders.USER_STATUS)).isNull();
                assertThat(wrapped.getHeader(UserContextHeaders.USER_CLIENT_IP)).isNull();
                assertThat(wrapped.getHeader(UserContextHeaders.USER_CLIENT_TYPE)).isNull();
                assertThat(wrapped.getHeader(UserContextHeaders.USER_DEVICE_ID)).isNull();
            });
        });

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(chain.called).isTrue();
    }

    @Test
    void shouldRunBeforeTokenFilterSoForgedHeadersAreRemovedBeforeTokenParsing() {

        assertThat(new AuthGlobalFilter().getOrder()).isLessThan(AuthTokenFilter.AUTH_TOKEN_FILTER_ORDER);
    }

    private UserDetails user() {

        return UserDetails.builder()
                .id(7L)
                .roles(List.of("admin", "user"))
                .permissions(List.of("order:create", "order:read"))
                .tenant(88L)
                .deptId(99L)
                .validated(true)
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
