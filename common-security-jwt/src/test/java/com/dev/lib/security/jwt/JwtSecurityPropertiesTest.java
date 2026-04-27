package com.dev.lib.security.jwt;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class JwtSecurityPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(JwtSecurityConfig.class);

    @Test
    void shouldBindJwtPropertiesInJwtModule() {

        contextRunner
                .withPropertyValues(
                        "app.security.jwt.secret=jwt-secret-value-with-enough-length",
                        "app.security.jwt.expiration=12345"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();

                    JwtSecurityProperties properties = context.getBean(JwtSecurityProperties.class);
                    assertThat(properties.getSecret()).isEqualTo("jwt-secret-value-with-enough-length");
                    assertThat(properties.getExpiration()).isEqualTo(12345L);
                });
    }
}
