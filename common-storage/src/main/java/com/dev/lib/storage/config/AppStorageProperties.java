package com.dev.lib.storage.config;

import com.dev.lib.storage.domain.model.StorageType;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

// 配置类
@Data
public class AppStorageProperties {

    private StorageType type;

    private String allowedExtensions;

    private Long maxSize;

    private Local local;

    private Oss oss;

    private Minio minio;

    private Rustfs rustfs;

    public StorageType getEffectiveType() {

        if (type != null) {
            validateConfigured(type);
            return type;
        }

        List<StorageType> configuredTypes = configuredTypes();
        if (configuredTypes.size() == 1) {
            return configuredTypes.getFirst();
        }

        if (configuredTypes.size() > 1) {
            throw new IllegalStateException(
                    "app.storage.type must be configured when multiple storage backends are configured: "
                            + configuredTypes
            );
        }

        return null;
    }

    public boolean isConfigured(StorageType storageType) {

        return switch (storageType) {
            case LOCAL -> local != null && hasText(local.path);
            case OSS -> oss != null
                    && hasText(oss.endpoint)
                    && hasText(oss.accessKey)
                    && hasText(oss.secretKey)
                    && hasText(oss.bucket);
            case MINIO -> minio != null
                    && hasText(minio.endpoint)
                    && hasText(minio.accessKey)
                    && hasText(minio.secretKey)
                    && hasText(minio.bucket);
            case RUSTFS -> rustfs != null
                    && hasText(rustfs.endpoint)
                    && hasText(rustfs.accessKey)
                    && hasText(rustfs.secretKey)
                    && hasText(rustfs.bucket);
        };
    }

    private List<StorageType> configuredTypes() {

        List<StorageType> configured = new ArrayList<>();
        for (StorageType candidate : StorageType.values()) {
            if (isConfigured(candidate)) {
                configured.add(candidate);
            }
        }
        return configured;
    }

    private void validateConfigured(StorageType configuredType) {

        if (!isConfigured(configuredType)) {
            throw new IllegalStateException("app.storage." + configuredType.name().toLowerCase()
                    + " must be fully configured when app.storage.type=" + configuredType.name().toLowerCase());
        }
    }

    private boolean hasText(String value) {

        return value != null && !value.isBlank();
    }

    @Data
    public static class Local {

        private String path;

        private String urlPrefix;

    }

    @Data
    public static class Oss {

        private String endpoint;

        private String accessKey;

        private String secretKey;

        private String bucket;

    }

    @Data
    @Deprecated
    public static class Minio {

        private String endpoint;

        private String accessKey;

        private String secretKey;

        private String bucket;

    }

    @Data
    public static class Rustfs {

        private String endpoint;

        private String accessKey;

        private String secretKey;

        private String bucket;

    }

}
