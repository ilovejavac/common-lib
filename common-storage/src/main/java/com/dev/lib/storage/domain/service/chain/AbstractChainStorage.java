package com.dev.lib.storage.domain.service.chain;

import com.dev.lib.storage.config.AppStorageProperties;
import com.dev.lib.storage.data.VfsPathRepository;
import com.dev.lib.storage.data.SysFile;
import com.dev.lib.storage.domain.service.virtual.StorageServiceNameProvider;
import com.dev.lib.storage.domain.service.write.SysFileCowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * ChainStorage 抽象基类
 * 提供数据库同步的通用逻辑
 *
 * 重构说明：
 * - 使用统一的 SysFileCowService 处理 COW 逻辑
 * - Storage 和 VFS 共享相同的版本管理机制
 * - 解决并发写入导致的文件丢失问题
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractChainStorage {

    protected final AppStorageProperties fileProperties;

    protected final VfsPathRepository fileRepository;

    protected final StorageServiceNameProvider serviceNameProvider;

    protected final SysFileCowService sysFileCowService;  // 统一 COW 服务

    // ==================== 数据库同步辅助方法（带 COW）====================

    /**
     * 保存文件记录到数据库（如果已存在则使用 COW 更新）
     *
     * @param bucketName  桶名称
     * @param objectKey   对象键
     * @param storagePath 存储路径
     * @param size        文件大小
     * @return SysFile 的 bizId
     */
    protected String saveFileRecord(String bucketName, String objectKey, String storagePath, Long size) {

        String serviceName = serviceNameProvider.currentServiceName();
        String virtualPath = bucketName + "/" + objectKey;
        String parentPath = extractParentPath(virtualPath);

        Optional<SysFile> existing = fileRepository.findByVirtualPathForUpdate(serviceName, virtualPath);
        SysFile savedFile;

        if (existing.isPresent()) {
            // 文件已存在，使用 COW 更新
            SysFile file = existing.get();

            // 使用统一的 COW 逻辑（通过模拟 InputStream）
            try (InputStream dummyStream = new ByteArrayInputStream(new byte[0])) {
                String fileName = extractFileName(objectKey);
                // 注意：这里 storagePath 是已经上传好的路径，不需要再次上传
                // 直接应用 COW 逻辑
                applyCOWForExistingFile(file, storagePath, size != null ? size : -1);
            } catch (IOException e) {
                log.warn("Failed to apply COW for file: {}", virtualPath, e);
                // 降级：直接覆盖
                file.setStoragePath(storagePath);
                if (size != null) {
                    file.setSize(size);
                }
            }

            if (file.getServiceName() == null || file.getServiceName().isBlank()) {
                file.setServiceName(serviceNameProvider.currentServiceName());
            }
            savedFile = fileRepository.save(file);
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
            file.setServiceName(serviceNameProvider.currentServiceName());
            savedFile = fileRepository.save(file);
        }
        return savedFile.getBizId();
    }

    /**
     * 为已存在的文件应用 COW 逻辑
     * 注意：storagePath 已经是上传后的路径，不需要再次上传
     */
    private void applyCOWForExistingFile(SysFile file, String newStoragePath, long newSize) {
        String oldStoragePath = file.getStoragePath();

        // 先添加旧版本（如果启用了 COW）
        if (oldStoragePath != null && !oldStoragePath.equals(newStoragePath)) {
            sysFileCowService.addOldVersion(file, oldStoragePath);
        }

        // 更新 storagePath 和 size
        file.setStoragePath(newStoragePath);
        if (newSize >= 0) {
            file.setSize(newSize);
        }
    }

    /**
     * 检查是否启用 COW
     */
    private boolean isCOWEnabled() {
        if (fileProperties.getVfs() == null) {
            return true;
        }
        Boolean cowEnabled = fileProperties.getVfs().getCowEnabled();
        return cowEnabled == null || cowEnabled;
    }

    /**
     * 保存文件记录到数据库（如果已存在则更新）
     * 使用 virtualPath 作为 storagePath（适用于 MinIO/OSS）
     *
     * @param bucketName 桶名称
     * @param objectKey  对象键
     * @param size       文件大小
     * @return SysFile 的 bizId
     */
    protected String saveFileRecord(String bucketName, String objectKey, Long size) {

        String virtualPath = bucketName + "/" + objectKey;
        return saveFileRecord(bucketName, objectKey, virtualPath, size);
    }

    /**
     * 更新文件记录（追加操作后更新大小）
     *
     * @param bucketName  桶名称
     * @param objectKey   对象键
     * @param storagePath 存储路径
     * @param newSize     新文件大小
     * @return SysFile 的 bizId
     */
    protected String updateFileRecord(String bucketName, String objectKey, String storagePath, long newSize) {

        String serviceName = serviceNameProvider.currentServiceName();
        String virtualPath = bucketName + "/" + objectKey;

        Optional<SysFile> existing = fileRepository.findByVirtualPath(serviceName, virtualPath);
        if (existing.isPresent()) {
            SysFile file = existing.get();
            file.setStoragePath(storagePath);
            file.setSize(newSize);
            SysFile savedFile = fileRepository.save(file);
            return savedFile.getBizId();
        } else {
            // 如果记录不存在，创建新记录
            return saveFileRecord(bucketName, objectKey, storagePath, newSize);
        }
    }

    /**
     * 删除文件记录
     *
     * @param bucketName 桶名称
     * @param objectKey  对象键
     */
    protected void deleteFileRecord(String bucketName, String objectKey) {

        String serviceName = serviceNameProvider.currentServiceName();
        String virtualPath = bucketName + "/" + objectKey;
        fileRepository.findByVirtualPath(serviceName, virtualPath).ifPresent(fileRepository::delete);
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
