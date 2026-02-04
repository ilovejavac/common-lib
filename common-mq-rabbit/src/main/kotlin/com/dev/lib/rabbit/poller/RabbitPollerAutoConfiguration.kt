package com.dev.lib.rabbit.poller

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * RabbitMQ Poller 自动配置
 * 启用 RabbitMQ Poller 配置属性
 */
@Configuration
@EnableConfigurationProperties(RabbitPollerProperties::class)
class RabbitPollerAutoConfiguration
