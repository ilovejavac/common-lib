package com.dev.lib.jpa.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

@Configuration  // 配置类注解在这里
@RequiredArgsConstructor
public class DataSourceProxyConfig {

//    private final SlowSqlQueryListener slowSqlQueryListener;
//
//    @Bean
//    @Primary
//    @ConditionalOnProperty(prefix = "app.sql-monitor", name = "enabled", havingValue = "true", matchIfMissing = false)
//    public DataSource dataSourceProxy(DataSource dataSource) {
//        return ProxyDataSourceBuilder
//                .create(dataSource)
//                .name("SlowSqlMonitor")
//                .listener(slowSqlQueryListener)
//                .multiline()
//                .build();
//    }
}