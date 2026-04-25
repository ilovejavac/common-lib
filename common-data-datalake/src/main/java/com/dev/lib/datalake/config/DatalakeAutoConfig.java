package com.dev.lib.datalake.config;

import com.dev.lib.datalake.ClickHouseRepositoryWritePlugin;
import com.dev.lib.datalake.DorisRepositoryWritePlugin;
import com.dev.lib.datalake.HiveRepositoryWritePlugin;
import com.dev.lib.jpa.CommonJpaAutoConfig;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(after = CommonJpaAutoConfig.class)
@EnableConfigurationProperties(DatalakeProperties.class)
public class DatalakeAutoConfig {

    @Bean
    @ConditionalOnProperty(prefix = "app.datalake.doris", name = "enabled", havingValue = "true", matchIfMissing = true)
    public DorisRepositoryWritePlugin dorisRepositoryWritePlugin(DatalakeProperties properties) {

        return new DorisRepositoryWritePlugin(properties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.datalake.clickhouse", name = "enabled", havingValue = "true", matchIfMissing = true)
    public ClickHouseRepositoryWritePlugin clickHouseRepositoryWritePlugin(DatalakeProperties properties) {

        return new ClickHouseRepositoryWritePlugin(properties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.datalake.hive", name = "enabled", havingValue = "true", matchIfMissing = true)
    public HiveRepositoryWritePlugin hiveRepositoryWritePlugin(DatalakeProperties properties) {

        return new HiveRepositoryWritePlugin(properties);
    }
}
