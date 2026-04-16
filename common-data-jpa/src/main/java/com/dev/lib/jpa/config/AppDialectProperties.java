package com.dev.lib.jpa.config;

import com.dev.lib.jpa.multiple.JpaDialect;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app")
public class AppDialectProperties {

    /**
     * Single-datasource JPA dialect shortcut.
     */
    private JpaDialect dialect = JpaDialect.AUTO;

    /**
     * Fallback for custom dialect implementations when the enum has no built-in mapping.
     */
    private String databasePlatform;
}
