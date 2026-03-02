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

    private Rustfs rustfs;

    private Vfs vfs = new Vfs();

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

    @Data
    public static class Vfs {

        /**
         * 临时文件默认过期时间（分钟）
         */
        private Long temporaryTtlMinutes = 60L;

        /**
         * 清理任务执行间隔（毫秒）
         */
        private Long cleanupIntervalMs = 300000L;

        /**
         * 清理任务初始延迟（毫秒）
         */
        private Long cleanupInitialDelayMs = 60000L;

    }

}
