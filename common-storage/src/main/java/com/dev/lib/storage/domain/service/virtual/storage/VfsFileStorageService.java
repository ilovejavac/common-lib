package com.dev.lib.storage.domain.service.virtual.storage;

import com.dev.lib.storage.Storage;
import com.dev.lib.storage.config.AppStorageProperties;
import com.dev.lib.storage.data.SysFile;
import com.dev.lib.storage.domain.service.chain.ChainStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * VFS 文件存储服务
 * <p>
 * 负责 VFS 与底层存储之间的桥接。
 * 使用 ChainStorageService 的纯 I/O 方法（putObject/copyObject 等），
 * 不触发 DB 同步——VFS 层自行管理 SysFile 记录。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VfsFileStorageService {

    private final VfsStoragePathManager pathManager;
    private final ChainStorageService chainStorage;

    // ==================== 上传操作 ====================

    /**
     * 上传文件内容（纯 I/O，不创建 SysFile 记录）
     *
     * @return 生成的 storagePath
     */
    public String upload(InputStream inputStream, String fileName, String storagePathPrefix) throws IOException {
        String storagePath = pathManager.generateStoragePath(fileName, storagePathPrefix);
        StorageRef ref = toStorageRef(storagePath);
        chainStorage.putObject(ref.bucket(), ref.objectKey(), inputStream);
        return storagePath;
    }

    /**
     * 上传文件内容（使用默认路径）
     */
    public String upload(InputStream inputStream, String fileName) throws IOException {
        return upload(inputStream, fileName, null);
    }

    /**
     * 追加内容并上传新文件（COW 模式：复制旧文件 + 追加新内容）
     *
     * @return 新的 storagePath
     */
    public String appendAndUpload(String oldStoragePath, byte[] appendBytes, String fileName) throws IOException {
        String newStoragePath = pathManager.generateStoragePath(fileName, null);
        StorageRef oldRef = toStorageRef(oldStoragePath);
        StorageRef newRef = toStorageRef(newStoragePath);

        chainStorage.copyObject(oldRef.bucket(), oldRef.objectKey(), newRef.objectKey());
        chainStorage.appendObject(newRef.bucket(), newRef.objectKey(), appendBytes);

        return newStoragePath;
    }

    // ==================== 下载操作 ====================

    /**
     * 下载文件
     */
    public InputStream download(String storagePath) throws IOException {
        StorageRef ref = toStorageRef(storagePath);
        return chainStorage.download(ref.bucket(), ref.objectKey());
    }

    // ==================== 复制操作 ====================

    /**
     * 复制文件到新路径（纯 I/O）
     */
    public void copy(String srcPath, String destPath) throws IOException {
        StorageRef srcRef = toStorageRef(srcPath);
        StorageRef dstRef = toStorageRef(destPath);
        chainStorage.copyObject(srcRef.bucket(), srcRef.objectKey(), dstRef.objectKey());
    }

    // ==================== 删除操作 ====================

    /**
     * 删除单个文件（纯 I/O）
     */
    public void delete(String storagePath) {
        StorageRef ref = toStorageRef(storagePath);
        try {
            chainStorage.removeObject(ref.bucket(), ref.objectKey());
        } catch (IOException e) {
            log.warn("Failed to delete storage object: {}", storagePath, e);
        }
    }

    /**
     * 批量删除文件
     */
    public void deleteAll(List<String> storagePaths) {
        if (storagePaths == null || storagePaths.isEmpty()) {
            return;
        }
        for (String path : storagePaths) {
            delete(path);
        }
    }

    // ==================== 文件信息收集 ====================

    /**
     * 收集文件的所有存储路径（当前路径 + 历史版本）
     */
    public List<String> collectStoragePaths(SysFile file) {
        List<String> paths = new ArrayList<>();
        if (Boolean.TRUE.equals(file.getIsDirectory())) {
            return paths;
        }
        if (file.getStoragePath() != null) {
            paths.add(file.getStoragePath());
        }
        if (file.getOldStoragePaths() != null) {
            paths.addAll(file.getOldStoragePaths());
        }
        return paths;
    }

    /**
     * 生成存储路径（用于复制操作）
     */
    public String generateStoragePath(String fileName, String storagePathPrefix) {
        return pathManager.generateStoragePath(fileName, storagePathPrefix);
    }

    public AppStorageProperties getStorageProperties() {
        return pathManager.getStorageProperties();
    }

    public String replaceLines(String storagePath, Storage.LineTransformer transformer) throws IOException {
        StorageRef ref = toStorageRef(storagePath);
        return chainStorage.replaceLines(ref.bucket(), ref.objectKey(), transformer);
    }

    public String getPresignedUrl(String storagePath, int expireSeconds) {
        StorageRef ref = toStorageRef(storagePath);
        return chainStorage.getPresignedUrl(ref.bucket(), ref.objectKey(), expireSeconds);
    }

    // ==================== 路径解析 ====================

    private String defaultBucket() {
        AppStorageProperties p = pathManager.getStorageProperties();
        if (p == null || p.getType() == null) {
            return "default";
        }

        return switch (p.getType()) {
            case LOCAL -> "default";
            case MINIO -> p.getMinio() != null && p.getMinio().getBucket() != null
                          ? p.getMinio().getBucket()
                          : "default";
            case OSS -> p.getOss() != null && p.getOss().getBucket() != null
                        ? p.getOss().getBucket()
                        : "default";
            case RUSTFS -> p.getRustfs() != null && p.getRustfs().getBucket() != null
                           ? p.getRustfs().getBucket()
                           : "default";
        };
    }

    private StorageRef toStorageRef(String storagePath) {
        String normalized = storagePath == null ? "" : storagePath.strip();
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }

        int idx = normalized.indexOf('/');
        if (idx > 0 && idx < normalized.length() - 1) {
            return new StorageRef(normalized.substring(0, idx), normalized.substring(idx + 1));
        }

        return new StorageRef(defaultBucket(), normalized);
    }

    private record StorageRef(String bucket, String objectKey) {}
}
