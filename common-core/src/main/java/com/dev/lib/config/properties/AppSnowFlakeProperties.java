package com.dev.lib.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.snow-flake")
public class AppSnowFlakeProperties {

    /**
     * Data center ID for Snowflake ID generation (0-15)
     */
    private Integer dataCenterId;

    /**
     * Worker ID for Snowflake ID generation (0-15)
     */
    private Integer workerId;

}
