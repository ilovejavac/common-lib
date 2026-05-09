package com.dev.lib.security.interceptor;

import com.dev.lib.config.properties.AppSecurityProperties;
import com.dev.lib.exceptions.BizException;
import com.dev.lib.security.config.SecurityMvcInterceptorRegistration;
import com.dev.lib.security.config.properties.SecurityValidProperties;
import com.dev.lib.security.service.PermissionService;
import com.dev.lib.security.service.TokenService;
import com.dev.lib.security.service.annotation.Anonymous;
import com.dev.lib.security.util.SecurityContextHolder;
import com.dev.lib.security.util.UserDetails;
import com.dev.lib.web.model.StandardErrorCodes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.handler.MappedInterceptor;
import org.springframework.web.util.ServletRequestPathUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminPathSecurityTest {

    @AfterEach
    void tearDown() {

        SecurityContextHolder.clear();
    }

    @Test
    void securityMvcInterceptorRegistrationShouldInterceptAdminAndApiPathsOnlyWithInternalAndAuth() throws Exception {

        PermissionValidator validator = validator(List.of());
        AuthInterceptor authInterceptor = new AuthInterceptor(validator);
        InternalInterceptor internalInterceptor = new InternalInterceptor(validator);
        SecurityMvcInterceptorRegistration registration = new SecurityMvcInterceptorRegistration(
                authInterceptor,
                internalInterceptor
        );
        InterceptorRegistry registry = new InterceptorRegistry();

        registration.addInterceptors(registry);

        List<MappedInterceptor> mappedInterceptors = mappedInterceptors(registry);
        assertThat(mappedInterceptors).hasSize(2);
        MappedInterceptor authMapping = mappingFor(mappedInterceptors, authInterceptor);
        MappedInterceptor internalMapping = mappingFor(mappedInterceptors, internalInterceptor);

        assertThat(authMapping.getIncludePathPatterns()).contains("/api/**", "/admin/**");
        assertThat(authMapping.getExcludePathPatterns()).contains("/api/auth/**", "/api/public/**");
        assertThat(internalMapping.getIncludePathPatterns()).contains("/api/**", "/admin/**");
        assertThat(internalMapping.getExcludePathPatterns()).contains("/api/auth/**", "/api/public/**");
        assertThat(authMapping.matches(request("/admin/demo"))).isTrue();
        assertThat(mappedInterceptors).containsExactly(internalMapping, authMapping);
    }

    @Test
    void adminPathWithoutLoginShouldFailAuthentication() {

        AuthInterceptor interceptor = interceptor(List.of());

        assertThatThrownBy(() -> interceptor.preHandle(
                request("/admin/demo"),
                new MockHttpServletResponse(),
                handler("admin")
        ))
                .isInstanceOf(BizException.class)
                .extracting("coder")
                .isEqualTo(StandardErrorCodes.AUTHENTICATION_FAILED);
    }

    @Test
    void adminPathWithUserRoleShouldFailPermission() {

        AuthInterceptor interceptor = interceptor(List.of("user"));
        MockHttpServletRequest request = request("/admin/demo");
        request.addHeader("Authorization", "Bearer token");

        assertThatThrownBy(() -> interceptor.preHandle(
                request,
                new MockHttpServletResponse(),
                handler("admin")
        ))
                .isInstanceOf(BizException.class)
                .extracting("coder")
                .isEqualTo(StandardErrorCodes.PERMISSION_DENIED);
    }

    @Test
    void adminPathShouldAcceptAdminRoleCaseInsensitively() throws Exception {

        assertAdminRolePasses("admin");
        assertAdminRolePasses("ADMIN");
        assertAdminRolePasses("Admin");
    }

    @Test
    void anonymousAnnotationShouldNotBypassAdminPath() {

        AuthInterceptor interceptor = interceptor(List.of());

        assertThatThrownBy(() -> interceptor.preHandle(
                request("/admin/demo"),
                new MockHttpServletResponse(),
                handler("anonymousAdmin")
        ))
                .isInstanceOf(BizException.class)
                .extracting("coder")
                .isEqualTo(StandardErrorCodes.AUTHENTICATION_FAILED);
    }

    @Test
    void whitelistShouldNotBypassAdminPath() {

        AuthInterceptor interceptor = interceptor(List.of(), Set.of("/admin/**"));

        assertThatThrownBy(() -> interceptor.preHandle(
                request("/admin/demo"),
                new MockHttpServletResponse(),
                handler("admin")
        ))
                .isInstanceOf(BizException.class)
                .extracting("coder")
                .isEqualTo(StandardErrorCodes.AUTHENTICATION_FAILED);
    }

    @Test
    void nonAdminPublicPathShouldKeepWhitelistBehavior() {

        AuthInterceptor interceptor = interceptor(List.of());

        assertThatCode(() -> interceptor.preHandle(
                request("/api/public/demo"),
                new MockHttpServletResponse(),
                handler("api")
        )).doesNotThrowAnyException();
    }

    private void assertAdminRolePasses(String role) throws Exception {

        AuthInterceptor interceptor = interceptor(List.of(role));
        MockHttpServletRequest request = request("/admin/demo");
        request.addHeader("Authorization", "Bearer token");

        assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), handler("admin"))).isTrue();

        SecurityContextHolder.clear();
    }

    private AuthInterceptor interceptor(List<String> roles) {

        return interceptor(roles, Set.of());
    }

    private AuthInterceptor interceptor(List<String> roles, Set<String> whitelist) {

        return new AuthInterceptor(validator(roles, whitelist));
    }

    private PermissionValidator validator(List<String> roles) {

        return validator(roles, Set.of());
    }

    private PermissionValidator validator(List<String> roles, Set<String> whitelist) {

        AppSecurityProperties securityProperties = new AppSecurityProperties();
        securityProperties.setWhiteListRequest(whitelist);
        PermissionValidator validator = new PermissionValidator(
                new ContextPermissionService(),
                new FixedTokenService(roles),
                new SecurityValidProperties(),
                securityProperties
        );
        validator.afterPropertiesSet();
        return validator;
    }

    private HandlerMethod handler(String methodName) throws NoSuchMethodException {

        return new HandlerMethod(new DemoController(), DemoController.class.getMethod(methodName));
    }

    private MockHttpServletRequest request(String uri) {

        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        request.setRequestURI(uri);
        request.setRemoteAddr("127.0.0.1");
        ServletRequestPathUtils.parseAndCache(request);
        return request;
    }

    private List<MappedInterceptor> mappedInterceptors(InterceptorRegistry registry) throws Exception {

        Method getInterceptors = InterceptorRegistry.class.getDeclaredMethod("getInterceptors");
        getInterceptors.setAccessible(true);
        return ((List<?>) getInterceptors.invoke(registry)).stream()
                .filter(MappedInterceptor.class::isInstance)
                .map(MappedInterceptor.class::cast)
                .toList();
    }

    private MappedInterceptor mappingFor(List<MappedInterceptor> mappedInterceptors, HandlerInterceptor interceptor) {

        return mappedInterceptors.stream()
                .filter(mapping -> mapping.getInterceptor() == interceptor)
                .findFirst()
                .orElseThrow();
    }

    static class DemoController {

        public void admin() {

        }

        public void api() {

        }

        @Anonymous
        public void anonymousAdmin() {

        }
    }

    private record FixedTokenService(List<String> roles) implements TokenService {

        @Override
        public String generateToken(UserDetails userDetails) {

            return "token";
        }

        @Override
        public UserDetails parseToken(String token) {

            return UserDetails.builder()
                    .id(1L)
                    .username("demo")
                    .roles(roles)
                    .validated(true)
                    .build();
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

}
