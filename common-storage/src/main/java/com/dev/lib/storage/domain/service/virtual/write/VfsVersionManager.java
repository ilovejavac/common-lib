package com.dev.lib.storage.domain.service.virtual.write;

import com.dev.lib.entity.id.IDWorker;
import com.dev.lib.storage.config.AppStorageProperties;
import com.dev.lib.storage.data.SysFile;
import com.dev.lib.storage.domain.model.VfsContext;
import com.dev.lib.storage.domain.service.virtual.StorageServiceNameProvider;
import com.dev.lib.storage.domain.service.virtual.directory.VfsDirectoryService;
import com.dev.lib.storage.domain.service.virtual.path.VfsPathResolver;
import com.dev.lib.storage.domain.service.virtual.repository.VfsFileRepository;
import com.dev.lib.storage.domain.service.virtual.storage.VfsFileStorageService;
import com.dev.lib.storage.domain.service.write.SysFileCowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * VFS 版本管理器
 * 负责文件的版本管理和 COW（写时复制）操作
 *
 * 重构说明：
 * - 使用统一的 SysFileCowService 处理 COW 逻辑
 * - 保留 VFS 特定的业务逻辑（临时文件、目录管理等）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VfsVersionManager {

    private static final long DEFAULT_TEMP_TTL_MINUTES = 60L;

    private final VfsFileRepository     fileRepository;

    private final VfsFileStorageService storageService;

    private final VfsDirectoryService   directoryService;

    private final VfsPathResolver       pathResolver;

    private final AppStorageProperties storageProperties;

    private final StorageServiceNameProvider serviceNameProvider;

    private final SysFileCowService sysFileCowService;  // 统一 COW 服务

    // ==================== 文件创建 ====================

    /**
     * 创建新文件（带版本管理）
     */
    @Transactional(rollbackFor = Exception.class)
    public String createFileWithVersioning(VfsContext ctx, String virtualPath, InputStream inputStream, long size) {

        String fileName  = pathResolver.getName(virtualPath);
        String extension = pathResolver.getExtension(fileName);

        try {
            String storagePath = storageService.upload(inputStream, fileName, null);

            SysFile file = new SysFile();
            file.setVirtualPath(virtualPath);
            file.setParentPath(pathResolver.getParent(virtualPath));
            file.setIsDirectory(false);
            file.setOriginalName(fileName);
            file.setStorageName(fileName);
            file.setStoragePath(storagePath);
            file.setExtension(extension);
            file.setSize(size);
            file.setStorageType(storageService.getStorageProperties().getType());
            file.setHidden(fileName.startsWith("."));
            file.setServiceName(serviceNameProvider.resolve(ctx));
            applyTemporaryMetadataForCreate(file, ctx);
            fileRepository.save(file);
            return file.getBizId();
        } catch (IOException e) {
            throw new RuntimeException("Failed to create file", e);
        }
    }

    // ==================== 文件更新（COW）====================

    /**
     * 执行带 COW 的写入操作
     * 重构：使用统一的 SysFileCowService
     */
    @Transactional(rollbackFor = Exception.class)
    public void writeWithCOW(VfsContext ctx, SysFile file, InputStream contentStream, long size, String fullPath) throws IOException {

        String fileName = pathResolver.getName(fullPath);

        // 使用统一的 COW 写入服务
        sysFileCowService.writeWithCOW(file, contentStream, size, fileName);

        // 应用 VFS 特定的元数据
        if (file.getServiceName() == null || file.getServiceName().isBlank()) {
            file.setServiceName(serviceNameProvider.resolve(ctx));
        }
        applyTemporaryMetadataForUpdate(file, ctx);

        fileRepository.save(file);
    }

    /**
     * 追加内容到文件
     * 重构：使用统一的 SysFileCowService
     */
    @Transactional(rollbackFor = Exception.class)
    public void appendContent(SysFile file, byte[] contentBytes, String fullPath) throws IOException {

        String fileName = pathResolver.getName(fullPath);

        // 使用统一的 COW 写入服务
        sysFileCowService.appendWithCOW(file, contentBytes, fileName);

        fileRepository.save(file);
    }

    private void applyTemporaryMetadataForCreate(SysFile file, VfsContext ctx) {

        boolean temporary = ctx != null && Boolean.TRUE.equals(ctx.getTemporary());
        file.setTemporary(temporary);

        if (temporary) {
            file.setExpirationAt(resolveExpirationAt(ctx));
        } else {
            file.setExpirationAt(null);
        }
    }

    private void applyTemporaryMetadataForUpdate(SysFile file, VfsContext ctx) {

        if (ctx == null || ctx.getTemporary() == null) {
            return;
        }

        if (Boolean.TRUE.equals(ctx.getTemporary())) {
            file.setTemporary(true);
            file.setExpirationAt(resolveExpirationAt(ctx));
            return;
        }

        file.setTemporary(false);
        file.setExpirationAt(null);
    }

    private LocalDateTime resolveExpirationAt(VfsContext ctx) {

        if (ctx != null && ctx.getExpirationAt() != null) {
            return ctx.getExpirationAt();
        }

        long ttlMinutes = DEFAULT_TEMP_TTL_MINUTES;
        if (storageProperties.getVfs() != null && storageProperties.getVfs().getTemporaryTtlMinutes() != null) {
            ttlMinutes = storageProperties.getVfs().getTemporaryTtlMinutes();
        }

        return LocalDateTime.now().plusMinutes(Math.max(ttlMinutes, 1L));
    }

    // ==================== 文件复制 ====================

    /**
     * 复制文件记录
     */
    @Transactional(rollbackFor = Exception.class)
    public void copyFileRecord(SysFile src, String destPath) {

        try {
            String newStoragePath = null;
            if (src.getStoragePath() != null) {
                newStoragePath = storageService.generateStoragePath(pathResolver.getName(destPath), null);
                storageService.copy(src.getStoragePath(), newStoragePath);
            }

            SysFile copy = new SysFile();
            copy.setBizId(IDWorker.newId());
            copy.setVirtualPath(destPath);
            copy.setParentPath(pathResolver.getParent(destPath));
            copy.setIsDirectory(false);
            copy.setOriginalName(pathResolver.getName(destPath));
            copy.setStorageName(src.getStorageName());
            copy.setStoragePath(newStoragePath);
            copy.setExtension(src.getExtension());
            copy.setContentType(src.getContentType());
            copy.setSize(src.getSize());
            copy.setStorageType(src.getStorageType());
            copy.setServiceName(
                    (src.getServiceName() == null || src.getServiceName().isBlank())
                    ? serviceNameProvider.currentServiceName()
                    : src.getServiceName()
            );
            fileRepository.save(copy);
        } catch (IOException e) {
            throw new RuntimeException("Failed to copy file content", e);
        }
    }

    /**
     * 递归复制目录
     */
    @Transactional(rollbackFor = Exception.class)
    public void copyDirectoryRecursive(VfsContext ctx, String srcDir, String destDir) {

        if (fileRepository.findByPath(destDir).isEmpty()) {
            directoryService.createDirectory(destDir);
        }

        List<SysFile> children = fileRepository.findChildren(srcDir);
        for (SysFile child : children) {
            String childName = pathResolver.getName(child.getVirtualPath());
            String destPath  = destDir + "/" + childName;

            if (Boolean.TRUE.equals(child.getIsDirectory())) {
                copyDirectoryRecursive(ctx, child.getVirtualPath(), destPath);
            } else {
                copyFileRecord(child, destPath);
            }
        }
    }

}
