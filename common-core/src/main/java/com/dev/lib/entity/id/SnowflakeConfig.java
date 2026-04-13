package com.dev.lib.entity.id;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Snowflake 配置类。
 *
 * <p>静态初始化块在类加载时（JVM 启动早期）自动从 YAML 读取配置并初始化
 * {@link SnowflakeDistributeId}，因此 {@link IDWorker} 无需等待 Spring 容器
 * 就可以安全调用，彻底消除初始化顺序问题。</p>
 */
@Slf4j
@Component
public class SnowflakeConfig {

    @Getter
    private static final SnowflakeDistributeId worker;

    static {
        long workerId = SnowflakeConfigLoader.getInstance().getWorkerId();
        worker = SnowflakeDistributeId.getInstance(workerId);
        log.info("[Snowflake] Initialized — workerId={}", workerId);
    }

    private SnowflakeConfig() {}
}
