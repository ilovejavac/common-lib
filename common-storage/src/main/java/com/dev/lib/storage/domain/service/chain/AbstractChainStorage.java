package com.dev.lib.storage.domain.service.chain;

import com.dev.lib.storage.config.AppStorageProperties;
import com.dev.lib.storage.data.FileSystemRepository;
import com.dev.lib.storage.data.SysFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * ChainStorage 抽象基类
 * 提供数据库同步的通用逻辑
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractChainStorage {

    protected final AppStorageProperties fileProperties;

    protected final FileSystemRepository fileRepository;

    // ==================== 数据库同步辅助方法 ====================

    /**
     * 保存文件记录到数据库（如果已存在则更新）
     *
     * @param bucketName  桶名称
     * @param objectKey   对象键
     * @param storagePath 存储路径
     * @param size        文件大小
     */
    protected void saveFileRecord(String bucketName, String objectKey, String storagePath, Long size) {

        String virtualPath = bucketName + "/" + objectKey;
        String parentPath = extractParentPath(virtualPath);

        Optional<SysFile> existing = fileRepository.findByVirtualPath(virtualPath);
        if (existing.isPresent()) {
            // 更新现有记录
            SysFile file = existing.get();
            file.setStoragePath(storagePath);
            if (size != null) {
                file.setSize(size);
            }
            fileRepository.save(file);
        } else {
            // 创建新记录
            SysFile file = new SysFile();
            file.setVirtualPath(virtualPath);
            file.setParentPath(parentPath);
            file.setStoragePath(storagePath);
            file.setStorageName(extractFileName(objectKey));
            file.setOriginalName(extractFileName(objectKey));
            if (size != null) {
                file.setSize(size);
            }
            file.setStorageType(fileProperties.getType());
            file.setIsDirectory(false);
            file.setHidden(extractFileName(objectKey).startsWith("."));
            file.setExtension(extractExtension(objectKey));
            fileRepository.save(file);
        }
    }

    /**
     * 保存文件记录到数据库（如果已存在则更新）
     * 使用 virtualPath 作为 storagePath（适用于 MinIO/OSS）
     *
     * @param bucketName 桶名称
     * @param objectKey  对象键
     * @param size       文件大小
     */
    protected void saveFileRecord(String bucketName, String objectKey, Long size) {

        String virtualPath = bucketName + "/" + objectKey;
        saveFileRecord(bucketName, objectKey, virtualPath, size);
    }

    /**
     * 更新文件记录（追加操作后更新大小）
     *
     * @param bucketName  桶名称
     * @param objectKey   对象键
     * @param storagePath 存储路径
     * @param newSize     新文件大小
     */
    protected void updateFileRecord(String bucketName, String objectKey, String storagePath, long newSize) {

        String virtualPath = bucketName + "/" + objectKey;

        Optional<SysFile> existing = fileRepository.findByVirtualPath(virtualPath);
        if (existing.isPresent()) {
            SysFile file = existing.get();
            file.setStoragePath(storagePath);
            file.setSize(newSize);
            fileRepository.save(file);
        } else {
            // 如果记录不存在，创建新记录
            saveFileRecord(bucketName, objectKey, storagePath, newSize);
        }
    }

    /**
     * 删除文件记录
     *
     * @param bucketName 桶名称
     * @param objectKey  对象键
     */
    protected void deleteFileRecord(String bucketName, String objectKey) {

        String virtualPath = bucketName + "/" + objectKey;
        fileRepository.findByVirtualPath(virtualPath).ifPresent(fileRepository::delete);
    }

    // ==================== 路径提取工具方法 ====================

    /**
     * 提取父路径
     */
    protected String extractParentPath(String virtualPath) {

        int lastSlash = virtualPath.lastIndexOf('/');
        if (lastSlash > 0) {
            return virtualPath.substring(0, lastSlash);
        }
        return "/";
    }

    /**
     * 提取文件名
     */
    protected String extractFileName(String objectKey) {

        int lastSlash = objectKey.lastIndexOf('/');
        if (lastSlash >= 0) {
            return objectKey.substring(lastSlash + 1);
        }
        return objectKey;
    }

    /**
     * 提取扩展名
     */
    protected String extractExtension(String objectKey) {

        String fileName = extractFileName(objectKey);
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            return fileName.substring(lastDot + 1).toLowerCase();
        }
        return null;
    }
}
