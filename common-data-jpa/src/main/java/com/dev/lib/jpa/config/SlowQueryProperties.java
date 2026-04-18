package com.dev.lib.jpa.config;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Setter
@Getter
@Accessors(chain = false, fluent = false)
@ConfigurationProperties(prefix = "app.jpa.slow-query")
public class SlowQueryProperties {

    private boolean enabled = true;

    @DurationUnit(ChronoUnit.MILLIS)
    private Duration threshold = Duration.ofMillis(120);

    private String loggerName = "com.dev.lib.jpa.slow-query";

}
