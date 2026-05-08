package com.dev.lib.aksk.domain.service;

import com.dev.lib.aksk.annotation.Aksk;
import com.dev.lib.aksk.config.AkskProperties;
import com.dev.lib.aksk.domain.model.AkskHeaders;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class AkskHeaderResolverTest {

    private final AkskHeaderResolver resolver = new AkskHeaderResolver();

    @Test
    void explicitAnnotationHeaderNamesShouldWinOverPrefixAndProperties() throws NoSuchMethodException {

        Aksk annotation = annotation("explicitHeaders");
        AkskProperties properties = new AkskProperties();
        properties.setHeaderPrefix("X-Configured");

        AkskHeaders headers = resolver.resolve(annotation, properties);

        assertThat(headers.accessKeyHeader()).isEqualTo("X-Explicit-AK");
        assertThat(headers.timestampHeader()).isEqualTo("X-Explicit-Timestamp");
        assertThat(headers.signatureHeader()).isEqualTo("X-Explicit-Signature");
    }

    @Test
    void annotationHeaderPrefixShouldDeriveThreeHeaders() throws NoSuchMethodException {

        Aksk annotation = annotation("prefixHeaders");

        AkskHeaders headers = resolver.resolve(annotation, new AkskProperties());

        assertThat(headers.accessKeyHeader()).isEqualTo("X-Datares-Access-Key");
        assertThat(headers.timestampHeader()).isEqualTo("X-Datares-Timestamp");
        assertThat(headers.signatureHeader()).isEqualTo("X-Datares-Signature");
    }

    @Test
    void emptyAnnotationValuesShouldFallBackToConfiguredHeaderNames() throws NoSuchMethodException {

        Aksk annotation = annotation("fallbackHeaders");
        AkskProperties properties = new AkskProperties();
        properties.setAccessKeyHeader("X-App-AK");
        properties.setTimestampHeader("X-App-Timestamp");
        properties.setSignatureHeader("X-App-Signature");

        AkskHeaders headers = resolver.resolve(annotation, properties);

        assertThat(headers.accessKeyHeader()).isEqualTo("X-App-AK");
        assertThat(headers.timestampHeader()).isEqualTo("X-App-Timestamp");
        assertThat(headers.signatureHeader()).isEqualTo("X-App-Signature");
    }

    @Test
    void propertiesDefaultPrefixShouldDeriveDefaultHeaders() throws NoSuchMethodException {

        Aksk annotation = annotation("fallbackHeaders");

        AkskHeaders headers = resolver.resolve(annotation, new AkskProperties());

        assertThat(headers.accessKeyHeader()).isEqualTo("X-Aksk-Access-Key");
        assertThat(headers.timestampHeader()).isEqualTo("X-Aksk-Timestamp");
        assertThat(headers.signatureHeader()).isEqualTo("X-Aksk-Signature");
    }

    private Aksk annotation(String methodName) throws NoSuchMethodException {

        Method method = SampleEndpoint.class.getDeclaredMethod(methodName);
        return method.getAnnotation(Aksk.class);
    }

    private static class SampleEndpoint {

        @Aksk(
                headerPrefix = "X-Ignored",
                accessKeyHeader = "X-Explicit-AK",
                timestampHeader = "X-Explicit-Timestamp",
                signatureHeader = "X-Explicit-Signature"
        )
        void explicitHeaders() {
        }

        @Aksk(headerPrefix = "X-Datares")
        void prefixHeaders() {
        }

        @Aksk
        void fallbackHeaders() {
        }
    }
}
