package com.dev.lib.aksk.domain.service;

import com.dev.lib.aksk.config.AkskProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AkskVerifierSpringContextTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(AkskVerifierApplication.class);

    @Test
    void shouldCreateVerifierBeanFromSpringContext() {

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(AkskVerifier.class);
        });
    }

    @SpringBootConfiguration
    @Import(AkskVerifier.class)
    static class AkskVerifierApplication {

        @Bean
        AkskService akskService() {

            return mock(AkskService.class);
        }

        @Bean
        AkskSigner akskSigner() {

            return new AkskSigner();
        }

        @Bean
        AkskProperties akskProperties() {

            return new AkskProperties();
        }
    }
}
