package com.dev.lib.security.interceptor;

import com.dev.lib.aksk.config.AkskProperties;
import com.dev.lib.aksk.domain.model.AkskAuthentication;
import com.dev.lib.aksk.domain.model.AkskVerificationRequest;
import com.dev.lib.aksk.domain.service.AkskHeaderResolver;
import com.dev.lib.aksk.domain.service.AkskSigner;
import com.dev.lib.aksk.domain.service.AkskVerifier;
import com.dev.lib.config.properties.AppSecurityProperties;
import com.dev.lib.security.aksk.AkskRequestBodyFilter;
import com.dev.lib.security.aksk.AkskSecurityContextAdapter;
import com.dev.lib.security.aksk.CachedBodyHttpServletRequest;
import com.dev.lib.security.config.WebSecurityConfig;
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

import java.io.IOException;
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
    void webSecurityConfigShouldRegisterAkskInterceptorBroadlyBeforeAuth() throws Exception {

        PermissionValidator validator = new PermissionValidator(
                new ContextPermissionService(),
                new EmptyTokenService(),
                new SecurityValidProperties(),
                new AppSecurityProperties()
        );
        validator.afterPropertiesSet();
        AuthInterceptor authInterceptor = new AuthInterceptor(validator);
        InternalInterceptor internalInterceptor = new InternalInterceptor(validator);
        AkskAuthenticationInterceptor akskInterceptor = new AkskAuthenticationInterceptor(
                new NoopVerifier(),
                new AkskHeaderResolver(),
                new AkskProperties(),
                new AkskSecurityContextAdapter()
        );
        WebSecurityConfig config = new WebSecurityConfig(authInterceptor, internalInterceptor, akskInterceptor);
        InterceptorRegistry registry = new InterceptorRegistry();

        config.addInterceptors(registry);

        List<MappedInterceptor> mappings = mappedInterceptors(registry);
        MappedInterceptor akskMapping = mappingFor(mappings, akskInterceptor);
        MappedInterceptor authMapping = mappingFor(mappings, authInterceptor);
        MockHttpServletRequest businessRequest = new MockHttpServletRequest("POST", "/partner/aksk");
        businessRequest.setRequestURI("/partner/aksk");
        ServletRequestPathUtils.parseAndCache(businessRequest);

        assertThat(akskMapping.getIncludePathPatterns()).containsExactly("/**");
        assertThat(akskMapping.matches(businessRequest)).isTrue();
        assertThat(mappings.indexOf(akskMapping)).isLessThan(mappings.indexOf(authMapping));
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

    private static class NoopVerifier extends AkskVerifier {

        private NoopVerifier() {

            super(null, new AkskSigner(), new AkskProperties());
        }

        @Override
        public AkskAuthentication verify(AkskVerificationRequest request) {

            return new AkskAuthentication();
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
