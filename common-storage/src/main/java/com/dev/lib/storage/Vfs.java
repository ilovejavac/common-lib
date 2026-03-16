package com.dev.lib.storage;

import com.dev.lib.storage.domain.api.VfsPath;
import com.dev.lib.storage.domain.model.VfsContext;
import com.dev.lib.storage.domain.service.virtual.core.VfsCoreDirectoryService;
import com.dev.lib.storage.domain.service.virtual.core.VfsFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * VFS 静态入口 - Linux 风格链式 API
 * <p>
 * 使用示例：
 * <pre>
 * // 读取文件并过滤
 * Vfs.path("/logs/app.log").cat().grep("ERROR").head(10).executeAsString();
 *
 * // 文本处理管道
 * Vfs.path("/data/file.csv").cat().cut(",", 1, 3).sort().uniq().writeTo("/data/result.csv");
 *
 * // 文件操作
 * Vfs.path("/data/file.txt").write("hello");
 * Vfs.path("/src").cp("/backup", true);
 * Vfs.path("/logs/old.log").rm(false);
 *
 * // 目录操作
 * Vfs.path("/logs/*.log").ls();
 * Vfs.path("/data").find("*.csv");
 * Vfs.path("/backup").mkdir(true);
 *
 * // 文件信息
 * Vfs.path("/data/file.txt").stat();
 * Vfs.path("/data/a.txt").diff("/data/b.txt");
 * </pre>
 */
@Component
@RequiredArgsConstructor
public class Vfs {

    private final VfsFileService fileService;
    private final VfsCoreDirectoryService directoryService;

    @Value("${spring.application.name:unknown-service}")
    private String applicationName;

    private static Vfs instance;
    private static String defaultServiceName = "unknown-service";

    @jakarta.annotation.PostConstruct
    public void init() {
        instance = this;
        defaultServiceName = (applicationName == null || applicationName.isBlank())
                             ? "unknown-service"
                             : applicationName;
    }

    // ==================== Linux 风格链式 API ====================

    /**
     * 链式 API 入口
     */
    public static VfsPath path(String path) {
        ensureInitialized();
        VfsContext ctx = VfsContext.of(null);
        ctx.setServiceName(defaultServiceName);
        return new VfsPath(ctx, path, instance.fileService, instance.directoryService);
    }

    /**
     * 链式 API 入口（带 root）
     */
    public static VfsPath path(String root, String path) {
        ensureInitialized();
        VfsContext ctx = VfsContext.of(root);
        ctx.setServiceName(defaultServiceName);
        return new VfsPath(ctx, path, instance.fileService, instance.directoryService);
    }

    /**
     * 链式 API 入口（带 context）
     */
    public static VfsPath path(VfsContext ctx, String path) {
        ensureInitialized();
        if (ctx.getServiceName() == null || ctx.getServiceName().isBlank()) {
            ctx.setServiceName(defaultServiceName);
        }
        return new VfsPath(ctx, path, instance.fileService, instance.directoryService);
    }

    private static void ensureInitialized() {
        if (instance == null) {
            throw new IllegalStateException("Vfs is not initialized");
        }
    }
}
