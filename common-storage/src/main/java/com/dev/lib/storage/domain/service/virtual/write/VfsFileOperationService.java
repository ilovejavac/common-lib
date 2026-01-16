package com.dev.lib.storage.domain.service.virtual.write;

import com.dev.lib.storage.data.SysFile;
import com.dev.lib.storage.domain.model.VfsContext;
import com.dev.lib.storage.domain.service.virtual.directory.VfsDirectoryService;
import com.dev.lib.storage.domain.service.virtual.path.VfsPathResolver;
import com.dev.lib.storage.domain.service.virtual.repository.VfsFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Optional;

/**
 * VFS 文件操作服务
 * 负责文件的写入、追加和创建操作
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VfsFileOperationService {

    private final VfsFileRepository fileRepository;

    private final VfsPathResolver pathResolver;

    private final VfsDirectoryService directoryService;

    private final VfsVersionManager versionManager;

    // ==================== 文件写入 ====================

    /**
     * 写入文件内容（字符串）
     */
    @Transactional(rollbackFor = Exception.class)
    public void writeFile(VfsContext ctx, String path, String content) {

        byte[] bytes      = content == null ? new byte[0] : content.getBytes(StandardCharsets.UTF_8);
        String fullPath   = pathResolver.resolve(ctx, path);
        String parentPath = pathResolver.getParent(fullPath);

        ensureParentExists(ctx, parentPath);

        Optional<SysFile> existing = fileRepository.findByPathForUpdate(fullPath);
        if (existing.isPresent()) {
            SysFile file = existing.get();
            validateIsFile(file, path);
            try (InputStream is = new ByteArrayInputStream(bytes)) {
                versionManager.writeWithCOW(file, is, bytes.length, fullPath);
            } catch (IOException e) {
                throw new RuntimeException("Failed to write file", e);
            }
        } else {
            try (InputStream is = new ByteArrayInputStream(bytes)) {
                versionManager.createFileWithVersioning(ctx, fullPath, is, bytes.length);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create file", e);
            }
        }
    }

    /**
     * 写入文件内容（输入流）
     */
    @Transactional(rollbackFor = Exception.class)
    public void writeFile(VfsContext ctx, String path, InputStream inputStream) {

        String fullPath   = pathResolver.resolve(ctx, path);
        String parentPath = pathResolver.getParent(fullPath);

        ensureParentExists(ctx, parentPath);

        Optional<SysFile> existing = fileRepository.findByPathForUpdate(fullPath);
        if (existing.isPresent()) {
            SysFile file = existing.get();
            validateIsFile(file, path);
            try {
                versionManager.writeWithCOW(file, inputStream, -1, fullPath);
            } catch (IOException e) {
                throw new RuntimeException("Failed to write file", e);
            }
        } else {
            versionManager.createFileWithVersioning(ctx, fullPath, inputStream, -1);
        }
    }

    /**
     * 追加内容到文件
     */
    @Transactional(rollbackFor = Exception.class)
    public void appendFile(VfsContext ctx, String path, String content) {

        String fullPath   = pathResolver.resolve(ctx, path);
        String parentPath = pathResolver.getParent(fullPath);

        ensureParentExists(ctx, parentPath);

        Optional<SysFile> existing     = fileRepository.findByPathForUpdate(fullPath);
        byte[]            contentBytes = content == null ? new byte[0] : content.getBytes(StandardCharsets.UTF_8);

        if (existing.isPresent()) {
            SysFile file = existing.get();
            validateIsFile(file, path);
            try {
                versionManager.appendContent(file, contentBytes, fullPath);
            } catch (IOException e) {
                throw new RuntimeException("Failed to append file", e);
            }
        } else {
            try (InputStream is = new ByteArrayInputStream(contentBytes)) {
                versionManager.createFileWithVersioning(ctx, fullPath, is, contentBytes.length);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create file", e);
            }
        }
    }

    /**
     * 创建空文件或更新文件时间戳
     */
    @Transactional(rollbackFor = Exception.class)
    public void touchFile(VfsContext ctx, String path) {

        String            fullPath = pathResolver.resolve(ctx, path);
        Optional<SysFile> existing = fileRepository.findByPathForUpdate(fullPath);

        if (existing.isPresent()) {
            SysFile file = existing.get();
            if (Boolean.TRUE.equals(file.getIsDirectory())) {
                throw new IllegalArgumentException("Cannot touch directory: " + path);
            }
            fileRepository.save(file);
        } else {
            try (InputStream is = new ByteArrayInputStream(new byte[0])) {
                versionManager.createFileWithVersioning(ctx, fullPath, is, 0);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create empty file", e);
            }
        }
    }

    /**
     * 创建目录
     */
    @Transactional(rollbackFor = Exception.class)
    public void createDirectory(VfsContext ctx, String path, boolean createParents) {

        String fullPath = pathResolver.resolve(ctx, path);

        if (fileRepository.findByPath(fullPath).isPresent()) {
            if (createParents) return;
            throw new IllegalArgumentException("Path already exists: " + path);
        }

        if (createParents) {
            directoryService.ensureDirs(ctx, fullPath, new HashSet<>());
        } else {
            String parentPath = pathResolver.getParent(fullPath);
            if (parentPath != null && !"/".equals(parentPath) && fileRepository.findByPath(parentPath).isEmpty()) {
                throw new IllegalArgumentException("Parent directory not found");
            }
            directoryService.createDirectory(fullPath);
        }
    }

    // ==================== 私有辅助方法 ====================

    private void ensureParentExists(VfsContext ctx, String parentPath) {

        if (parentPath != null && !"/".equals(parentPath) && fileRepository.findByPath(parentPath).isEmpty()) {
            directoryService.ensureDirs(ctx, parentPath, new HashSet<>());
        }
    }

    private void validateIsFile(SysFile file, String path) {

        if (Boolean.TRUE.equals(file.getIsDirectory())) {
            throw new IllegalArgumentException("Cannot write to directory: " + path);
        }
    }

}
