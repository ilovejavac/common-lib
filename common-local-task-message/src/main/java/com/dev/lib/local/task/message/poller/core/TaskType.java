package com.dev.lib.local.task.message.poller.core;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * 任务类型注解
 * 用于标识 PollerTaskExecutor 实现类对应的任务类型
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

}
