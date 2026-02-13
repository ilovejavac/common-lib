package com.dev.lib.local.task.message.poller.core;

import com.dev.lib.local.task.message.poller.strategy.BackoffStrategy;
import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * 任务类型注解
 * 用于标识 PollerTaskExecutor 实现类对应的任务类型及配置
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface TaskType {

    /**
     * 任务类型（如 "RABBIT_RETRY", "HTTP_NOTIFY"）
     */
    String value();

    /**
     * 是否启用，默认 true
     */
    boolean enabled() default true;

    /**
     * 门牌号数量（用于并发执行），默认 1
     */
    int houseNumberCount() default 1;

    /**
     * 轮询间隔（字符串格式，如 "10s", "30s", "1m"）
     */
    String pollInterval() default "1m";

    /**
     * 每次从数据库获取的最大任务数，默认 100
     */
    int fetchLimit() default 100;

    /**
     * 最大重试次数，默认 3
     */
    int maxRetry() default 3;

    /**
     * 基础延迟时间（字符串格式，如 "1s", "500ms"），默认 "1s"
     */
    String baseDelay() default "1s";

    /**
     * 最大延迟时间（字符串格式，如 "5m", "300s"），默认 "5m"
     */
    String maxDelay() default "5m";

    /**
     * 退避策略，默认 LINEAR
     */
    BackoffStrategy backoffStrategy() default BackoffStrategy.LINEAR;

    /**
     * 任务超时时间（分钟），默认 5
     * 超过此时间未完成的任务会被重置为 PENDING
     */
    int timeoutMinutes() default 5;

}
