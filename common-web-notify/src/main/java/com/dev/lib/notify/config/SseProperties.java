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
     * SSE 连接超时时间（毫秒）
     * <p>
     * 默认 30 分钟，适合大多数场景
     * <p>
     * LLM 流式场景建议设置更长：
     * - 1 小时 = 3600000
     * - 24 小时 = 86400000
     * - 永不过期 = Long.MAX_VALUE
     */
    private Long timeout = 0L;

    /**
     * 心跳间隔时间（秒）
     * <p>
     * 建议设置为超时时间的 1/3 到 1/2
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
