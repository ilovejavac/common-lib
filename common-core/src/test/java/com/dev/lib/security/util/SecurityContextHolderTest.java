package com.dev.lib.security.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityContextHolderTest {

    @AfterEach
    void tearDown() {

        SecurityContextHolder.clear();
    }

    @Test
    void setShouldRequireActiveScope() {

        assertThatThrownBy(() -> SecurityContextHolder.set(user("demo")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void emptyScopeShouldAllowTemporaryContextMutation() {

        SecurityContextHolder.withEmptyContext(() -> {
            assertThat(SecurityContextHolder.isLogin()).isFalse();

            SecurityContextHolder.set(user("demo"));

            assertThat(SecurityContextHolder.current().getUsername()).isEqualTo("demo");

            SecurityContextHolder.clear();

            assertThat(SecurityContextHolder.isLogin()).isFalse();
        });

        assertThat(SecurityContextHolder.isLogin()).isFalse();
    }

    private UserDetails user(String username) {

        return UserDetails.builder()
                .id(1L)
                .username(username)
                .validated(true)
                .build();
    }
}
