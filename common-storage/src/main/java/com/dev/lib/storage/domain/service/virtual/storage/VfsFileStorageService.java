package com.dev.lib.storage.domain.service.virtual.storage;

import com.dev.lib.storage.Storage;
import com.dev.lib.storage.config.AppStorageProperties;
import com.dev.lib.storage.data.SysFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * VFS 文件存储服务
 * 负责文件的上传、下载、复制、删除等存储操作
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VfsFileStorageService {

    private final VfsStoragePathManager pathManager;

    // ==================== 上传操作 ====================

    /**
     * 上传文件内容
     */
    public String upload(InputStream inputStream, String fileName, String storagePathPrefix) throws IOException {

        String storagePath = pathManager.generateStoragePath(fileName, storagePathPrefix);
        StorageRef ref = toStorageRef(storagePath);
        Storage.bucket(ref.bucket()).object(ref.objectKey()).write(inputStream);
        return storagePath;
    }

    /**
     * 上传文件内容（使用默认路径）
     */
    public String upload(InputStream inputStream, String fileName) throws IOException {

        return upload(inputStream, fileName, null);
    }

    /**
     * 追加内容并上传新文件（COW 模式）
     */
    public String appendAndUpload(String oldStoragePath, byte[] appendBytes, String fileName) throws IOException {

        String newStoragePath = pathManager.generateStoragePath(fileName, null);
        StorageRef oldRef = toStorageRef(oldStoragePath);
        StorageRef newRef = toStorageRef(newStoragePath);
        Storage.bucket(oldRef.bucket()).object(oldRef.objectKey()).copy(newRef.objectKey());

        String content = new String(appendBytes, StandardCharsets.UTF_8);
        Storage.bucket(newRef.bucket()).object(newRef.objectKey()).append(content);

        return newStoragePath;
    }

    // ==================== 下载操作 ====================

    /**
     * 下载文件
     */
    public InputStream download(String storagePath) throws IOException {

        StorageRef ref = toStorageRef(storagePath);
        return Storage.bucket(ref.bucket()).object(ref.objectKey()).download();
    }

    // ==================== 复制操作 ====================

    /**
     * 复制文件到新路径
     */
    public void copy(String srcPath, String destPath) throws IOException {

        StorageRef srcRef = toStorageRef(srcPath);
        StorageRef dstRef = toStorageRef(destPath);
        Storage.bucket(srcRef.bucket()).object(srcRef.objectKey()).copy(dstRef.objectKey());
    }

    // ==================== 删除操作 ====================

    /**
     * 删除单个文件
     */
    public void delete(String storagePath) {

        StorageRef ref = toStorageRef(storagePath);
        Storage.bucket(ref.bucket()).object(ref.objectKey()).delete();
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
        return Storage.bucket(ref.bucket()).object(ref.objectKey()).replaceLines(transformer);
    }

    public String getPresignedUrl(String storagePath, int expireSeconds) {

        StorageRef ref = toStorageRef(storagePath);
        return Storage.bucket(ref.bucket()).object(ref.objectKey()).presignedUrl(expireSeconds);
    }

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

        if (pathManager.getStorageProperties().getType() == com.dev.lib.storage.domain.model.StorageType.LOCAL) {
            int idx = normalized.indexOf('/');
            if (idx > 0 && idx < normalized.length() - 1) {
                return new StorageRef(normalized.substring(0, idx), normalized.substring(idx + 1));
            }
        }

        return new StorageRef(defaultBucket(), normalized);
    }

    private record StorageRef(String bucket, String objectKey) {}

}
