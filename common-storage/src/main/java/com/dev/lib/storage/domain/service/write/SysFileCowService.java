package com.dev.lib.storage.domain.service.write;

import com.dev.lib.storage.config.AppStorageProperties;
import com.dev.lib.storage.data.SysFile;
import com.dev.lib.storage.domain.service.virtual.storage.VfsFileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * SysFile COW（Copy-On-Write）服务
 * 为 Storage 和 VFS 提供统一的写时复制机制
 *
 * 核心职责：
 * 1. COW 写入：每次写入创建新文件，保留旧版本
 * 2. 版本管理：最多保留 10 个旧版本，自动清理
 * 3. 延迟删除：5 分钟后异步清理，防止并发读取失败
 * 4. 配置开关：支持禁用 COW（storage.vfs.cow-enabled=false）
 *
 * 使用场景：
 * - VFS 写入：Vfs.path("/bucket/data.txt").write("content")
 * - Storage 写入：Storage.bucket("bucket").object("data.txt").write("content")
 * - 两者共享相同的 COW 逻辑，避免并发写入时文件丢失
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SysFileCowService {

    private static final int MAX_OLD_VERSIONS = 10;
    private static final int VERSIONS_TO_DELETE = 5;
    private static final long DELAY_DELETE_MINUTES = 5L;

    private final ObjectProvider<VfsFileStorageService> storageServiceProvider;
    private final AppStorageProperties storageProperties;

    // ==================== 写入操作（COW）====================

    /**
     * 写入文件内容（带 COW）
     *
     * @param file 文件记录（必须已存在）
     * @param contentStream 内容流
     * @param size 文件大小（-1 表示未知）
     * @param fileName 文件名（用于生成存储路径）
     * @return 新的 storagePath
     * @throws IOException 写入失败
     */
    @Transactional(rollbackFor = Exception.class)
    public String writeWithCOW(SysFile file, InputStream contentStream, long size, String fileName) throws IOException {

        if (file == null) {
            throw new IllegalArgumentException("file must not be null");
        }

        String oldStoragePath = file.getStoragePath();
        String newStoragePath = storageService().upload(contentStream, fileName, null);

        // 应用 COW 逻辑
        applyCOW(file, oldStoragePath, newStoragePath, size);

        return newStoragePath;
    }

    /**
     * 追加内容到文件（带 COW）
     *
     * @param file 文件记录
     * @param contentBytes 追加的内容
     * @param fileName 文件名
     * @return 新的 storagePath
     * @throws IOException 追加失败
     */
    @Transactional(rollbackFor = Exception.class)
    public String appendWithCOW(SysFile file, byte[] contentBytes, String fileName) throws IOException {

        if (file == null) {
            throw new IllegalArgumentException("file must not be null");
        }

        String oldStoragePath = file.getStoragePath();
        String newStoragePath = storageService().appendAndUpload(
                file.getStoragePath(),
                contentBytes,
                fileName
        );

        long newSize = file.getSize() == null ? contentBytes.length : file.getSize() + contentBytes.length;
        applyCOW(file, oldStoragePath, newStoragePath, newSize);

        return newStoragePath;
    }

    // ==================== COW 核心逻辑 ====================

    /**
     * 应用 COW 逻辑
     * 1. 更新 storagePath 为新路径
     * 2. 将旧路径加入 oldStoragePaths
     * 3. 管理版本数量（超过限制时删除最老的版本）
     * 4. 设置延迟删除时间
     *
     * @param file 文件记录
     * @param oldStoragePath 旧存储路径
     * @param newStoragePath 新存储路径
     * @param newSize 新文件大小
     */
    private void applyCOW(SysFile file, String oldStoragePath, String newStoragePath, long newSize) {

        // 检查是否启用 COW
        if (!isCOWEnabled()) {
            log.debug("COW disabled, directly update storagePath");
            file.setStoragePath(newStoragePath);
            if (newSize >= 0) {
                file.setSize(newSize);
            }
            return;
        }

        // 更新文件信息
        file.setStoragePath(newStoragePath);
        if (newSize >= 0) {
            file.setSize(newSize);
        }

        // 如果旧路径和新路径相同，不需要版本管理
        if (oldStoragePath == null || oldStoragePath.equals(newStoragePath)) {
            return;
        }

        // 管理旧版本
        manageOldVersions(file, oldStoragePath);
    }

    /**
     * 管理旧版本文件
     * 1. 将旧路径加入 oldStoragePaths
     * 2. 如果超过最大版本数，删除最老的版本
     * 3. 设置延迟删除时间
     */
    private void manageOldVersions(SysFile file, String oldStoragePath) {

        List<String> oldPaths = file.getOldStoragePaths();
        if (oldPaths == null) {
            oldPaths = new ArrayList<>();
        }

        // 检查是否超过最大版本数
        if (oldPaths.size() >= MAX_OLD_VERSIONS) {
            log.info("Old versions exceed limit ({}), deleting oldest {} versions",
                    MAX_OLD_VERSIONS, VERSIONS_TO_DELETE);

            List<String> toDelete = oldPaths.subList(0, VERSIONS_TO_DELETE);
            storageService().deleteAll(new ArrayList<>(toDelete));
            toDelete.clear();
        }

        // 添加旧版本
        oldPaths.add(oldStoragePath);
        file.setOldStoragePaths(oldPaths);

        // 设置延迟删除时间（防止并发读取失败）
        file.setDeleteAfter(LocalDateTime.now().plusMinutes(DELAY_DELETE_MINUTES));
    }

    // ==================== 配置检查 ====================

    /**
     * 检查是否启用 COW
     * 默认启用，可通过配置关闭
     */
    private boolean isCOWEnabled() {
        if (storageProperties.getVfs() == null) {
            return true;
        }
        Boolean cowEnabled = storageProperties.getVfs().getCowEnabled();
        return cowEnabled == null || cowEnabled;
    }

    // ==================== 版本清理 ====================

    /**
     * 立即清理文件的所有旧版本
     * 用于手动清理或测试
     *
     * @param file 文件记录
     */
    @Transactional(rollbackFor = Exception.class)
    public void cleanupOldVersions(SysFile file) {

        if (file == null || file.getOldStoragePaths() == null || file.getOldStoragePaths().isEmpty()) {
            return;
        }

        log.info("Cleaning up {} old versions for file: {}", file.getOldStoragePaths().size(), file.getVirtualPath());

        storageService().deleteAll(new ArrayList<>(file.getOldStoragePaths()));
        file.setOldStoragePaths(null);
        file.setDeleteAfter(null);
    }

    /**
     * 手动添加旧版本到文件记录
     * 用于 Storage API 的 COW 支持
     *
     * @param file 文件记录
     * @param oldStoragePath 旧存储路径
     */
    public void addOldVersion(SysFile file, String oldStoragePath) {

        if (file == null || oldStoragePath == null || oldStoragePath.isBlank()) {
            return;
        }

        // 如果 COW 未启用，直接返回
        if (!isCOWEnabled()) {
            return;
        }

        // 管理旧版本
        manageOldVersions(file, oldStoragePath);
    }

    private VfsFileStorageService storageService() {

        VfsFileStorageService storageService = storageServiceProvider.getIfAvailable();
        if (storageService == null) {
            throw new IllegalStateException("VfsFileStorageService not available");
        }
        return storageService;
    }
}
