package com.dev.lib.jpa.config;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@Accessors(chain = false, fluent = false)
@ConfigurationProperties(prefix = "app.jpa.hikari-defaults")
public class JpaHikariDefaultsProperties {

    private Integer maximumPoolSize;

    private Integer minimumIdle;

    private Long connectionTimeout;

    private Long validationTimeout;

    private Long idleTimeout;

    private Long maxLifetime;

    private Long keepaliveTime;
}
