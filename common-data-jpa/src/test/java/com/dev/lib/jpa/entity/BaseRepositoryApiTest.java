package com.dev.lib.jpa.entity;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class BaseRepositoryApiTest {

    @Test
    void shouldExposeOnlyExplicitRepositoryOperations() {

        assertThat(JpaRepository.class.isAssignableFrom(BaseRepository.class)).isFalse();

        Set<String> methodNames = Arrays.stream(BaseRepository.class.getMethods())
                .map(Method::getName)
                .collect(Collectors.toSet());

        assertThat(methodNames).containsExactlyInAnyOrder(
                "save",
                "saveAll",
                "delete",
                "load",
                "loads",
                "page",
                "loadForUpdate",
                "update"
        );
        assertThat(Arrays.stream(BaseRepository.class.getMethods())
                .filter(method -> method.getName().equals("load")))
                .hasSize(2);
    }
}
