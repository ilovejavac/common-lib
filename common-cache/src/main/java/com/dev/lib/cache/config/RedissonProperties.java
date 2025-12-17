package com.dev.lib.cache.config;

import lombok.Data;

@Data
public class RedissonProperties {

    private String host;

    private int port;

    private String password;

    private int database;

}
