package com.dev.lib.storage.domain.service.virtual;

import com.dev.lib.storage.config.AppStorageProperties;
import com.dev.lib.storage.domain.model.VfsContext;
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

    public String resolve(VfsContext ctx) {

        if (ctx != null && ctx.getServiceName() != null && !ctx.getServiceName().isBlank()) {
            return ctx.getServiceName();
        }
        return applicationName;
    }

}
