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

    private VirtualFile vfs = new VirtualFile();

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
    public static class VirtualFile {

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

        /**
         * 是否启用 COW（Copy-On-Write）机制
         * 默认启用，禁用后写入操作将直接覆盖文件，不保留旧版本
         *
         * 启用 COW 的优点：
         * - 并发安全：写入时不影响正在读取的旧版本
         * - 版本管理：保留最多 10 个旧版本
         * - 延迟删除：5 分钟后异步清理，防止并发读取失败
         *
         * 禁用 COW 的场景：
         * - 不需要版本管理
         * - 追求极致性能
         * - 存储空间受限
         */
        private Boolean cowEnabled = true;

    }

}
