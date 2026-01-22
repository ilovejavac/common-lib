package com.dev.lib.storage.domain.service.virtual.write;

import com.dev.lib.entity.id.IDWorker;
import com.dev.lib.storage.data.SysFile;
import com.dev.lib.storage.domain.model.VfsContext;
import com.dev.lib.storage.domain.service.virtual.directory.VfsDirectoryService;
import com.dev.lib.storage.domain.service.virtual.path.VfsPathResolver;
import com.dev.lib.storage.domain.service.virtual.repository.VfsFileRepository;
import com.dev.lib.storage.domain.service.virtual.storage.VfsFileStorageService;
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
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VfsVersionManager {

    private static final int MAX_OLD_VERSIONS   = 10;

    private static final int VERSIONS_TO_DELETE = 5;

    private final VfsFileRepository     fileRepository;

    private final VfsFileStorageService storageService;

    private final VfsDirectoryService   directoryService;

    private final VfsPathResolver       pathResolver;

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
            fileRepository.save(file);
            return file.getBizId();
        } catch (IOException e) {
            throw new RuntimeException("Failed to create file", e);
        }
    }

    // ==================== 文件更新（COW）====================

    /**
     * 执行带 COW 的写入操作
     */
    @Transactional(rollbackFor = Exception.class)
    public void writeWithCOW(SysFile file, InputStream contentStream, long size, String fullPath) throws IOException {

        String oldStoragePath = file.getStoragePath();
        String newStoragePath = storageService.upload(contentStream, pathResolver.getName(fullPath), null);

        updateFileWithVersioning(file, newStoragePath, size, oldStoragePath);
    }

    /**
     * 追加内容到文件
     */
    @Transactional(rollbackFor = Exception.class)
    public void appendContent(SysFile file, byte[] contentBytes, String fullPath) throws IOException {

        String oldStoragePath = file.getStoragePath();
        String newStoragePath = storageService.appendAndUpload(
                file.getStoragePath(),
                contentBytes,
                pathResolver.getName(fullPath)
        );

        long newSize = file.getSize() == null ? contentBytes.length : file.getSize() + contentBytes.length;
        updateFileWithVersioning(file, newStoragePath, newSize, oldStoragePath);
    }

    /**
     * 更新文件并管理旧版本
     */
    private void updateFileWithVersioning(SysFile file, String newStoragePath, long newSize, String oldStoragePath) {

        file.setStoragePath(newStoragePath);
        file.setSize(newSize);

        if (oldStoragePath != null && !oldStoragePath.equals(newStoragePath)) {
            manageOldVersions(file, oldStoragePath);
        }

        fileRepository.save(file);
    }

    /**
     * 管理旧版本文件
     */
    private void manageOldVersions(SysFile file, String oldStoragePath) {

        List<String> oldPaths = file.getOldStoragePaths();
        if (oldPaths == null) {
            oldPaths = new ArrayList<>();
        }

        if (oldPaths.size() >= MAX_OLD_VERSIONS) {
            List<String> toDelete = oldPaths.subList(0, VERSIONS_TO_DELETE);
            storageService.deleteAll(new ArrayList<>(toDelete));
            toDelete.clear();
        }

        oldPaths.add(oldStoragePath);
        file.setOldStoragePaths(oldPaths);
        file.setDeleteAfter(LocalDateTime.now().plusMinutes(5));
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
