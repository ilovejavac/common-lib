package com.dev.lib.storage.domain.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 统一解析存储记录的服务归属名称。
 */
@Component
public class StorageServiceNameProvider {

    private final String applicationName;

    public StorageServiceNameProvider(@Value("${spring.application.name:unknown-service}") String applicationName) {

        this.applicationName = applicationName;
    }

    public String currentServiceName() {

        return applicationName;
    }
}
