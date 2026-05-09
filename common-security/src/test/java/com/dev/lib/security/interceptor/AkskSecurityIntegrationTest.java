package com.dev.lib.security.interceptor;

import com.dev.lib.aksk.config.AkskProperties;
import com.dev.lib.aksk.web.AkskRequestBodyFilter;
import com.dev.lib.aksk.web.CachedBodyHttpServletRequest;
import com.dev.lib.config.properties.AppSecurityProperties;
import com.dev.lib.security.config.SecurityMvcInterceptorRegistration;
import com.dev.lib.security.config.properties.SecurityValidProperties;
import com.dev.lib.security.service.PermissionService;
import com.dev.lib.security.service.TokenService;
import com.dev.lib.security.util.SecurityContextHolder;
import com.dev.lib.security.util.UserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.handler.MappedInterceptor;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AkskSecurityIntegrationTest {

    @AfterEach
    void tearDown() {

        SecurityContextHolder.clear();
    }

    @Test
    void requestBodyFilterShouldCacheBodyAndPreserveRepeatedReads() throws Exception {

        AkskProperties properties = new AkskProperties();
        AkskRequestBodyFilter filter = new AkskRequestBodyFilter(properties);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/partner/aksk");
        request.setContent("payload".getBytes(StandardCharsets.UTF_8));
        CapturingChain chain = new CapturingChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(chain.request).isInstanceOf(CachedBodyHttpServletRequest.class);
        CachedBodyHttpServletRequest cached = (CachedBodyHttpServletRequest) chain.request;
        assertThat(cached.getCachedBody()).isEqualTo("payload".getBytes(StandardCharsets.UTF_8));
        assertThat(cached.getInputStream().readAllBytes()).isEqualTo("payload".getBytes(StandardCharsets.UTF_8));
        assertThat(cached.getInputStream().readAllBytes()).isEqualTo("payload".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void requestBodyFilterShouldSkipOversizedBody() throws Exception {

        AkskProperties properties = new AkskProperties();
        properties.setMaxCachedBodyBytes(3);
        AkskRequestBodyFilter filter = new AkskRequestBodyFilter(properties);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/partner/aksk");
        request.setContent("payload".getBytes(StandardCharsets.UTF_8));
        CapturingChain chain = new CapturingChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(chain.request).isSameAs(request);
    }

    @Test
    void securityRegistrationShouldRegisterOnlyInternalAndAuthInterceptors() throws Exception {

        PermissionValidator validator = new PermissionValidator(
                new ContextPermissionService(),
                new EmptyTokenService(),
                new SecurityValidProperties(),
                new AppSecurityProperties()
        );
        validator.afterPropertiesSet();
        AuthInterceptor authInterceptor = new AuthInterceptor(validator);
        InternalInterceptor internalInterceptor = new InternalInterceptor(validator);
        SecurityMvcInterceptorRegistration registration = new SecurityMvcInterceptorRegistration(
                authInterceptor,
                internalInterceptor
        );
        InterceptorRegistry registry = new InterceptorRegistry();

        registration.addInterceptors(registry);

        List<MappedInterceptor> mappings = mappedInterceptors(registry);
        assertThat(mappings).hasSize(2);
        MappedInterceptor internalMapping = mappingFor(mappings, internalInterceptor);
        MappedInterceptor authMapping = mappingFor(mappings, authInterceptor);
        MockHttpServletRequest publicRequest = new MockHttpServletRequest("POST", "/partner/aksk");
        publicRequest.setRequestURI("/partner/aksk");
        ServletRequestPathUtils.parseAndCache(publicRequest);

        assertThat(internalMapping.getIncludePathPatterns()).containsExactly("/api/**", "/admin/**");
        assertThat(authMapping.getIncludePathPatterns()).containsExactly("/api/**", "/admin/**");
        assertThat(internalMapping.matches(publicRequest)).isFalse();
        assertThat(authMapping.matches(publicRequest)).isFalse();
        assertThat(mappings).containsExactly(internalMapping, authMapping);
    }

    @Test
    void legacySecurityAkskWebRuntimeShouldNotBeComponentScanned() {

        assertThat(com.dev.lib.security.interceptor.AkskAuthenticationInterceptor.class.isAnnotationPresent(Component.class))
                .isFalse();
        assertThat(com.dev.lib.security.aksk.AkskRequestBodyFilter.class.isAnnotationPresent(Component.class))
                .isFalse();
    }

    @Test
    void legacySecurityAkskWebRuntimeShouldBeDeprecatedBridgesToCommonAkskRuntime() {

        assertThat(com.dev.lib.aksk.web.AkskAuthenticationInterceptor.class.isAssignableFrom(
                com.dev.lib.security.interceptor.AkskAuthenticationInterceptor.class
        )).isTrue();
        assertThat(com.dev.lib.aksk.web.AkskRequestBodyFilter.class.isAssignableFrom(
                com.dev.lib.security.aksk.AkskRequestBodyFilter.class
        )).isTrue();
        assertThat(com.dev.lib.aksk.web.CachedBodyHttpServletRequest.class.isAssignableFrom(
                com.dev.lib.security.aksk.CachedBodyHttpServletRequest.class
        )).isTrue();
        assertThat(com.dev.lib.security.interceptor.AkskAuthenticationInterceptor.class.isAnnotationPresent(Deprecated.class))
                .isTrue();
        assertThat(com.dev.lib.security.aksk.AkskRequestBodyFilter.class.isAnnotationPresent(Deprecated.class))
                .isTrue();
        assertThat(com.dev.lib.security.aksk.CachedBodyHttpServletRequest.class.isAnnotationPresent(Deprecated.class))
                .isTrue();
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

    private static class CapturingChain implements FilterChain {

        private ServletRequest request;

        @Override
        public void doFilter(ServletRequest request, ServletResponse response) {

            this.request = request;
        }
    }

    private static class EmptyTokenService implements TokenService {

        @Override
        public String generateToken(UserDetails userDetails) {

            return "token";
        }

        @Override
        public UserDetails parseToken(String token) {

            return null;
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
