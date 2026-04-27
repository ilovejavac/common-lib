package com.dev.lib.security.jwt;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenServiceTest {

    @Test
    void shouldRejectMissingJwtSecret() {

        JwtSecurityProperties properties = new JwtSecurityProperties();
        JwtTokenService service = new JwtTokenService(
                properties,
                null,
                null
        );

        assertThatThrownBy(service::afterPropertiesSet)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("app.security.jwt.secret must be configured when using JWT security");
    }
}
