package com.dev.lib.storage.domain.service.virtual.storage;

import com.dev.lib.storage.config.AppStorageProperties;
import com.dev.lib.storage.data.SysFile;
import com.dev.lib.storage.domain.service.StorageService;
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

    private final StorageService        storageService;

    private final VfsStoragePathManager pathManager;

    // ==================== 上传操作 ====================

    /**
     * 上传文件内容
     */
    public String upload(InputStream inputStream, String fileName, String storagePathPrefix) throws IOException {

        String storagePath = pathManager.generateStoragePath(fileName, storagePathPrefix);
        storageService.upload(inputStream, storagePath);
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
        storageService.copy(oldStoragePath, newStoragePath);

        String content = new String(appendBytes, StandardCharsets.UTF_8);
        storageService.append(newStoragePath, content);

        return newStoragePath;
    }

    // ==================== 下载操作 ====================

    /**
     * 下载文件
     */
    public InputStream download(String storagePath) throws IOException {

        return storageService.download(storagePath);
    }

    // ==================== 复制操作 ====================

    /**
     * 复制文件到新路径
     */
    public void copy(String srcPath, String destPath) throws IOException {

        storageService.copy(srcPath, destPath);
    }

    // ==================== 删除操作 ====================

    /**
     * 删除单个文件
     */
    public void delete(String storagePath) {

        storageService.delete(storagePath);
    }

    /**
     * 批量删除文件
     */
    public void deleteAll(List<String> storagePaths) {

        storageService.deleteAll(storagePaths);
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

    // ==================== Getter ====================

    public StorageService getStorageService() {

        return storageService;
    }

    public AppStorageProperties getStorageProperties() {

        return pathManager.getStorageProperties();
    }

}
