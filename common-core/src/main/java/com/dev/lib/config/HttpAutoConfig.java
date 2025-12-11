package com.dev.lib.config;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class HttpAutoConfig {

    @Bean
    public OkHttpClient okHttpClient() {

        ConnectionPool pool = new ConnectionPool(
                10,
                5,
                TimeUnit.MINUTES
        );

        return new OkHttpClient.Builder()
                .connectionPool(pool)
                .retryOnConnectionFailure(true)
                .connectTimeout(
                        100,
                        TimeUnit.SECONDS
                )
                .readTimeout(
                        100,
                        TimeUnit.SECONDS
                )
                .writeTimeout(
                        100,
                        TimeUnit.SECONDS
                )
                .build();
    }

}
