package com.dev.lib.security.interceptor;

import com.dev.lib.aksk.domain.model.AkskAuthentication;
import com.dev.lib.security.aksk.AkskSecurityContextAdapter;
import com.dev.lib.security.aksk.SecurityAkskAuthenticationSuccessHandler;
import com.dev.lib.config.properties.AppSecurityProperties;
import com.dev.lib.security.config.properties.SecurityValidProperties;
import com.dev.lib.security.service.PermissionService;
import com.dev.lib.security.service.TokenService;
import com.dev.lib.security.util.SecurityContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.util.ServletRequestPathUtils;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class AkskAuthenticationInterceptorTest {

    @AfterEach
    void tearDown() {

        SecurityContextHolder.clear();
    }

    @Test
    void akskSuccessHandlerShouldWriteValidatedUserIntoSecurityContext() {

        SecurityAkskAuthenticationSuccessHandler successHandler = new SecurityAkskAuthenticationSuccessHandler(
                new AkskSecurityContextAdapter()
        );

        successHandler.onSuccess(authentication(), request("/api/partner"));

        assertThat(SecurityContextHolder.current().getUsername()).isEqualTo("partner-code");
        assertThat(SecurityContextHolder.current().getRealName()).isEqualTo("Partner System");
        assertThat(SecurityContextHolder.current().getValidated()).isTrue();
        assertThat(SecurityContextHolder.getRoles()).containsExactly("AKSK");
        assertThat(SecurityContextHolder.getPermissions()).containsExactly("method:scope");
        assertThat(SecurityContextHolder.current().getClientIp()).isEqualTo("127.0.0.1");
        assertThat(SecurityContextHolder.current().getExtra()).containsEntry("accessKey", "ak_partner");
    }

    @Test
    void successfulAkskAuthenticationShouldSatisfyAuthInterceptorWithoutBearerToken() throws Exception {

        SecurityAkskAuthenticationSuccessHandler successHandler = new SecurityAkskAuthenticationSuccessHandler(
                new AkskSecurityContextAdapter()
        );
        successHandler.onSuccess(authentication(), request("/api/partner"));
        AuthInterceptor authInterceptor = new AuthInterceptor(validator());
        MockHttpServletRequest request = request("/api/partner");
        HandlerMethod handler = handler(new ApiController(), "api");

        assertThatCode(() -> authInterceptor.preHandle(
                request,
                new MockHttpServletResponse(),
                handler
        )).doesNotThrowAnyException();

        authInterceptor.afterCompletion(request, new MockHttpServletResponse(), handler, null);
        assertThat(SecurityContextHolder.isLogin()).isFalse();
    }

    private MockHttpServletRequest request(String uri) {

        MockHttpServletRequest request = new MockHttpServletRequest("POST", uri);
        request.setRequestURI(uri);
        request.setRemoteAddr("127.0.0.1");
        ServletRequestPathUtils.parseAndCache(request);
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

    private PermissionValidator validator() {

        PermissionValidator validator = new PermissionValidator(
                new ContextPermissionService(),
                new EmptyTokenService(),
                new SecurityValidProperties(),
                new AppSecurityProperties()
        );
        validator.afterPropertiesSet();
        return validator;
    }

    static class ApiController {

        public void api() {

        }
    }

    private static class ContextPermissionService implements PermissionService {

        @Override
        public boolean hasPermission(String... permissions) {

            return Arrays.stream(permissions)
                    .allMatch(SecurityContextHolder::hasPermission);
        }

        @Override
        public boolean hasRole(String... roles) {

            return Arrays.stream(roles)
                    .allMatch(SecurityContextHolder::hasRole);
        }
    }

    private static class EmptyTokenService implements TokenService {

        @Override
        public String generateToken(com.dev.lib.security.util.UserDetails userDetails) {

            return "token";
        }

        @Override
        public com.dev.lib.security.util.UserDetails parseToken(String token) {

            return null;
        }
    }
}
