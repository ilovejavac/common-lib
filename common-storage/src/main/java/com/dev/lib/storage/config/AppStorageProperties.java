package com.dev.lib.storage.config;


import com.dev.lib.storage.domain.model.StorageType;
import lombok.Data;

// 配置类
@Data
public class AppStorageProperties {
    private StorageType type;
    private String allowedExtensions;
    private Long maxSize;

    private Local local;
    private Oss oss;
    private Minio minio;

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
    public static class Minio {
        private String endpoint;
        private String accessKey;
        private String secretKey;
        private String bucket;
    }
}