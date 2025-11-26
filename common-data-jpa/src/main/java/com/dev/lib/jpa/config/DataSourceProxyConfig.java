package com.dev.lib.jpa.config;

import com.dev.lib.jpa.entity.sql.SlowSqlQueryListener;
import lombok.RequiredArgsConstructor;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration  // 配置类注解在这里
@RequiredArgsConstructor
public class DataSourceProxyConfig {

    private final SlowSqlQueryListener slowSqlQueryListener;

    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "app.sql-monitor", name = "enabled", havingValue = "true", matchIfMissing = false)
    public DataSource dataSourceProxy(DataSource dataSource) {
        return ProxyDataSourceBuilder
                .create(dataSource)
                .name("SlowSqlMonitor")
                .listener(slowSqlQueryListener)
                .multiline()
                .build();
    }
}