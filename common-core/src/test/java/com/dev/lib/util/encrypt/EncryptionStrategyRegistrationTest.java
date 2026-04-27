package com.dev.lib.util.encrypt;

import com.dev.lib.config.properties.AppSecurityProperties;
import com.dev.lib.entity.encrypt.EncryptionService;
import com.dev.lib.util.encrypt.factory.EncryptionStrategyFactory;
import com.dev.lib.util.encrypt.impl.AesGcmEncryptionStrategy;
import com.dev.lib.util.encrypt.impl.Base64EncryptionStrategy;
import com.dev.lib.util.encrypt.impl.CustomEncryptionStrategy;
import com.dev.lib.util.encrypt.impl.RSAEncryptionStrategy;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class EncryptionStrategyRegistrationTest {

    private static final String AES_SECRET = Base64.getEncoder().encodeToString(new byte[32]);

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(EncryptionConfig.class);

    @Test
    void shouldRegisterConfiguredDecryptorsButEncryptWithCurrentVersion() {

        contextRunner
                .withPropertyValues(
                        "app.security.encrypt-version=aes",
                        "app.security.aes.secret=" + AES_SECRET,
                        "app.security.base64.enabled=true"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(AesGcmEncryptionStrategy.class);
                    assertThat(context).hasSingleBean(Base64EncryptionStrategy.class);
                    assertThat(context).doesNotHaveBean(RSAEncryptionStrategy.class);

                    EncryptionStrategyFactory factory = context.getBean(EncryptionStrategyFactory.class);
                    assertThat(factory.hasStrategy("v1")).isTrue();
                    assertThat(factory.hasStrategy("v2")).isTrue();
                    assertThat(factory.hasStrategy("v3")).isFalse();

                    EncryptionService service = context.getBean(EncryptionService.class);
                    String legacyCipherText = Base64.getEncoder()
                            .encodeToString("legacy".getBytes(StandardCharsets.UTF_8));

                    assertThat(service.decrypt("v1:" + legacyCipherText)).isEqualTo("legacy");
                    assertThat(service.encrypt("current")).startsWith("v2:");
                });
    }

    @Test
    void shouldFailWhenRsaIsExplicitlyEnabledWithoutConfiguredKeys() {

        contextRunner
                .withPropertyValues(
                        "app.security.encrypt-version=aes",
                        "app.security.aes.secret=" + AES_SECRET,
                        "app.security.rsa.enabled=true"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseMessage(
                                    "app.security.rsa.public-key and app.security.rsa.private-key must both be configured when RSA encryption is enabled"
                            );
                });
    }

    @Test
    void shouldRegisterRsaDecryptorWhenRsaPropertiesAreConfigured() throws Exception {

        KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();

        contextRunner
                .withPropertyValues(
                        "app.security.encrypt-version=base64",
                        "app.security.rsa.public-key=" + Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()),
                        "app.security.rsa.private-key=" + Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded())
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(Base64EncryptionStrategy.class);
                    assertThat(context).hasSingleBean(RSAEncryptionStrategy.class);
                    assertThat(context).doesNotHaveBean(AesGcmEncryptionStrategy.class);

                    EncryptionStrategyFactory factory = context.getBean(EncryptionStrategyFactory.class);
                    assertThat(factory.hasStrategy("v1")).isTrue();
                    assertThat(factory.hasStrategy("v2")).isFalse();
                    assertThat(factory.hasStrategy("v3")).isTrue();

                    EncryptionService service = context.getBean(EncryptionService.class);
                    assertThat(service.encrypt("current")).startsWith("v1:");
                });
    }

    @Test
    void shouldNotRegisterDecryptorWhenEnabledFlagIsFalse() {

        contextRunner
                .withPropertyValues(
                        "app.security.encrypt-version=aes",
                        "app.security.aes.secret=" + AES_SECRET,
                        "app.security.base64.enabled=false"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(AesGcmEncryptionStrategy.class);
                    assertThat(context).doesNotHaveBean(Base64EncryptionStrategy.class);

                    EncryptionStrategyFactory factory = context.getBean(EncryptionStrategyFactory.class);
                    assertThat(factory.hasStrategy("v1")).isFalse();
                    assertThat(factory.hasStrategy("v2")).isTrue();
                });
    }

    @Test
    void shouldUseOnlyRegisteredStrategyForEncryptionWhenEncryptVersionIsMissing() {

        contextRunner
                .withPropertyValues("app.security.aes.secret=" + AES_SECRET)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(AesGcmEncryptionStrategy.class);
                    assertThat(context).doesNotHaveBean(Base64EncryptionStrategy.class);

                    EncryptionService service = context.getBean(EncryptionService.class);
                    assertThat(service.encrypt("current")).startsWith("v2:");
                });
    }

    @Test
    void shouldFailWhenEncryptVersionIsMissingAndMultipleStrategiesAreRegistered() {

        contextRunner
                .withPropertyValues(
                        "app.security.aes.secret=" + AES_SECRET,
                        "app.security.base64.enabled=true"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .rootCause()
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessageContaining(
                                    "app.security.encrypt-version must be configured when multiple encryption strategies are registered"
                            )
                            .hasMessageContaining("AES")
                            .hasMessageContaining("BASE64");
                });
    }

    @Configuration
    @EnableConfigurationProperties(AppSecurityProperties.class)
    @Import({
            EncryptionStrategyFactory.class,
            EncryptionServiceImpl.class,
            AesGcmEncryptionStrategy.class,
            Base64EncryptionStrategy.class,
            RSAEncryptionStrategy.class,
            CustomEncryptionStrategy.class
    })
    static class EncryptionConfig {
    }
}
