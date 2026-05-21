package org.example.commonlib.aksk;

import com.dev.lib.aksk.data.AkskCredential;
import com.dev.lib.aksk.config.AkskProperties;
import com.dev.lib.aksk.web.AkskAuthenticationInterceptor;
import com.dev.lib.aksk.web.AkskMvcInterceptorRegistration;
import com.dev.lib.aksk.web.AkskRequestBodyFilter;
import com.dev.lib.config.properties.AppSecurityProperties;
import com.dev.lib.util.encrypt.EncryptionServiceImpl;
import com.dev.lib.util.encrypt.factory.EncryptionStrategyFactory;
import com.dev.lib.util.encrypt.impl.Base64EncryptionStrategy;
import com.dev.lib.web.interceptor.CommonMvcInterceptorRegistration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

class AkskWebAutoConfigurationIntegrationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withUserConfiguration(BusinessApplication.class)
            .withPropertyValues(
                    "spring.datasource.url=jdbc:h2:mem:aksk_web_auto_config;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                    "spring.datasource.driver-class-name=org.h2.Driver",
                    "spring.datasource.username=sa",
                    "spring.datasource.password=",
                    "spring.jpa.hibernate.ddl-auto=create-drop",
                    "spring.jpa.open-in-view=false",
                    "spring.application.name=aksk-web-auto-config-test",
                    "app.security.encrypt-version=base64"
            );

    private final WebApplicationContextRunner componentScanContextRunner = new WebApplicationContextRunner()
            .withUserConfiguration(ComponentScanBusinessApplication.class)
            .withPropertyValues(
                    "spring.datasource.url=jdbc:h2:mem:aksk_web_auto_config_scan;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                    "spring.datasource.driver-class-name=org.h2.Driver",
                    "spring.datasource.username=sa",
                    "spring.datasource.password=",
                    "spring.jpa.hibernate.ddl-auto=create-drop",
                    "spring.jpa.open-in-view=false",
                    "spring.application.name=aksk-web-auto-config-scan-test",
                    "app.security.encrypt-version=base64"
            );

    @Test
    void shouldRegisterAkskWebRuntimeWhenBusinessApplicationPackageIsOutsideCommonLib() {

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(AkskProperties.class);
            assertThat(context).hasSingleBean(AkskCredential.Mapper.class);
            assertThat(context).hasSingleBean(AkskAuthenticationInterceptor.class);
            assertThat(context).hasSingleBean(AkskMvcInterceptorRegistration.class);
            assertThat(context).hasSingleBean(AkskRequestBodyFilter.class);
            assertThat(context).getBeans(CommonMvcInterceptorRegistration.class)
                    .containsKey("akskMvcInterceptorRegistration");
        });
    }

    @Test
    void shouldKeepSingleAkskPropertiesBeanWhenConsumerAlsoScansAkskPackage() {

        componentScanContextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(AkskProperties.class);
            assertThat(context).hasSingleBean(AkskRequestBodyFilter.class);
        });
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableConfigurationProperties(AppSecurityProperties.class)
    @Import({
            EncryptionStrategyFactory.class,
            EncryptionServiceImpl.class,
            Base64EncryptionStrategy.class
    })
    static class BusinessApplication {
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @ComponentScan(basePackages = "com.dev.lib.aksk")
    @EnableConfigurationProperties(AppSecurityProperties.class)
    @Import({
            EncryptionStrategyFactory.class,
            EncryptionServiceImpl.class,
            Base64EncryptionStrategy.class
    })
    static class ComponentScanBusinessApplication {
    }
}
