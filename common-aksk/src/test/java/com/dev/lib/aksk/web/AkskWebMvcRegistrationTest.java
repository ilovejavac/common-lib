package com.dev.lib.aksk.web;

import com.dev.lib.aksk.config.AkskProperties;
import com.dev.lib.aksk.domain.model.AkskAuthentication;
import com.dev.lib.aksk.domain.model.AkskVerificationRequest;
import com.dev.lib.aksk.domain.service.AkskHeaderResolver;
import com.dev.lib.aksk.domain.service.AkskSigner;
import com.dev.lib.aksk.domain.service.AkskVerifier;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.handler.MappedInterceptor;
import org.springframework.web.util.ServletRequestPathUtils;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AkskWebMvcRegistrationTest {

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
        assertThat(request.getAttribute(AkskRequestBodyFilter.BODY_CACHE_SKIPPED_ATTRIBUTE)).isEqualTo(true);
    }

    @Test
    void registrationShouldMapAkskInterceptorBroadlyAtExpectedOrder() throws Exception {

        HandlerInterceptor before = new HandlerInterceptor() {
        };
        AkskAuthenticationInterceptor aksk = new AkskAuthenticationInterceptor(
                new NoopVerifier(),
                new AkskHeaderResolver(),
                new AkskProperties(),
                List.of()
        );
        HandlerInterceptor after = new HandlerInterceptor() {
        };
        AkskMvcInterceptorRegistration registration = new AkskMvcInterceptorRegistration(aksk);
        InterceptorRegistry registry = new InterceptorRegistry();

        registry.addInterceptor(before).order(10).addPathPatterns("/before/**");
        registration.addInterceptors(registry);
        registry.addInterceptor(after).order(20).addPathPatterns("/after/**");

        List<MappedInterceptor> mappings = mappedInterceptors(registry);
        MappedInterceptor akskMapping = mappingFor(mappings, aksk);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/partner/aksk");
        request.setRequestURI("/partner/aksk");
        ServletRequestPathUtils.parseAndCache(request);

        assertThat(akskMapping.getIncludePathPatterns()).containsExactly("/**");
        assertThat(akskMapping.matches(request)).isTrue();
        assertThat(mappings.indexOf(akskMapping)).isGreaterThan(mappings.indexOf(mappingFor(mappings, before)));
        assertThat(mappings.indexOf(akskMapping)).isLessThan(mappings.indexOf(mappingFor(mappings, after)));
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
}
