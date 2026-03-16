package com.dev.lib;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = StarterWithoutDatabaseTest.TestConfig.class)
class StarterWithoutDatabaseTest {

    @Test
    void shouldStartWithoutDatabaseConfiguration() {
        // 验证可以使用 Page 对象，不需要数据库配置
        Page<String> page = new PageImpl<>(
                List.of("item1", "item2"),
                PageRequest.of(0, 10),
                2
        );

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Configuration
    static class TestConfig {
        // 空配置，只测试 Spring 上下文能否启动
    }
}
