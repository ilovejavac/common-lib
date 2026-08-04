package com.dev.lib.cache;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CacheSerializationOwnershipTest {

    @Test
    void shouldLeaveRedissonSerializationToApplicationConfiguration() {

        assertThatThrownBy(() -> Class.forName("com.dev.lib.cache.JacksonRedissonCodec"))
                .isInstanceOf(ClassNotFoundException.class);
        assertThatThrownBy(() -> Class.forName("com.dev.lib.cache.config.RedissonConfig"))
                .isInstanceOf(ClassNotFoundException.class);
    }
}
