package com.dev.lib.notify.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * SSE 配置属性
 */
@Data
@ConfigurationProperties(prefix = "sse")
public class SseProperties {

    /**
     * 心跳间隔时间（秒）
     */
    private Integer heartbeatInterval = 30;

    /**
     * 是否启用连接
     * <p>
     * 可通过配置关闭 SSE 功能
     */
    private Boolean enabled = true;

    /**
     * 最大连接数
     * <p>
     * 防止资源耗尽，0 表示无限制
     */
    private Integer maxConnections = 0;

}
