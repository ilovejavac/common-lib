package com.dev.lib.jpa.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.jpa")
public class CommonJpaProperties {

    /**
     * Batch size for IN-clause based select/update/delete operations.
     */
    private int inClauseBatchSize = 1000;
}
