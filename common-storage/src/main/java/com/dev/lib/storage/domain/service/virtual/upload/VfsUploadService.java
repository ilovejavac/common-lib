package com.dev.lib.storage.domain.service.virtual.upload;

import com.dev.lib.storage.domain.model.VfsContext;
import com.dev.lib.storage.domain.service.virtual.directory.VfsDirectoryService;
import com.dev.lib.storage.domain.service.virtual.path.VfsPathResolver;
import com.dev.lib.storage.domain.service.virtual.repository.VfsFileRepository;
import com.dev.lib.storage.domain.service.virtual.write.VfsVersionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * VFS 上传服务
 * 负责 ZIP 文件和多文件的上传操作
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VfsUploadService {

    private final VfsFileRepository   fileRepository;

    private final VfsPathResolver     pathResolver;

    private final VfsDirectoryService directoryService;

    private final VfsVersionManager   versionManager;

    // ==================== ZIP 上传 ====================

    /**
     * 上传并解压 ZIP 文件
     */
    @Transactional(rollbackFor = Exception.class)
    public List<String> uploadZip(VfsContext ctx, String path, InputStream zipStream) {

        String basePath = pathResolver.resolve(ctx, path);

        ensureBaseDirectoryExists(ctx, basePath);

        List<String> fileIds = new ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(zipStream)) {
            ZipEntry    entry;
            Set<String> createdDirs = new HashSet<>();
            createdDirs.add(basePath);

            while ((entry = zis.getNextEntry()) != null) {
                String entryPath = pathResolver.normalize(basePath + "/" + entry.getName());

                if (entry.isDirectory()) {
                    ensureDirectoryExists(ctx, entryPath, createdDirs);
                } else {
                    ensureParentDirectoryExists(ctx, entryPath, createdDirs);
                    validateFileNotExists(entryPath);
                    String bizId = versionManager.createFileWithVersioning(ctx, entryPath, zis, entry.getSize());
                    fileIds.add(bizId);
                    zis.closeEntry();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to extract zip", e);
        }
        return fileIds;
    }

    // ==================== 单文件上传 ====================

    /**
     * 上传单个文件
     *
     * @param ctx        VFS 上下文
     * @param path       目标文件路径
     * @param inputStream 文件输入流
     * @param size       文件大小（字节）
     * @return 创建的文件 bizId
     */
    @Transactional(rollbackFor = Exception.class)
    public String uploadFile(VfsContext ctx, String path, InputStream inputStream, long size) {

        String fullPath = pathResolver.resolve(ctx, path);

        ensureParentDirectoryExists(ctx, fullPath, new HashSet<>());
        validateFileNotExists(fullPath);

        return versionManager.createFileWithVersioning(ctx, fullPath, inputStream, size);
    }

    // ==================== 多文件上传 ====================

    /**
     * 上传多个文件（基于 MultipartFile）
     *
     * @param ctx          VFS 上下文
     * @param targetPath   目标路径
     * @param files        要上传的文件数组
     * @param relativePaths 相对路径数组（可选），长度必须与 files 相同
     * @return 创建的文件 bizId 列表
     * @throws IllegalArgumentException 如果 relativePaths 长度与 files 长度不匹配
     */
    @Transactional(rollbackFor = Exception.class)
    public List<String> uploadFiles(VfsContext ctx, String targetPath, MultipartFile[] files, String[] relativePaths) {

        // 输入验证
        if (files == null || files.length == 0) {
            throw new IllegalArgumentException("Files array must not be null or empty");
        }
        if (relativePaths != null && relativePaths.length != files.length) {
            throw new IllegalArgumentException(
                    "relativePaths length must match files length: expected " + files.length + ", got " + relativePaths.length);
        }

        String basePath = pathResolver.resolve(ctx, targetPath);

        ensureBaseDirectoryExists(ctx, basePath);

        Set<String> createdDirs = new HashSet<>();
        createdDirs.add(basePath);
        List<String> fileIds = new ArrayList<>();

        String storagePathPrefix = resolveStoragePathPrefix(targetPath);

        try {
            for (int i = 0; i < files.length; i++) {
                MultipartFile file         = files[i];
                String        relativePath = getRelativePath(relativePaths, i, file);

                if (relativePath == null || relativePath.isEmpty()) continue;

                String fullPath = pathResolver.normalize(basePath + "/" + relativePath);
                ensureParentDirectoryForUpload(ctx, fullPath, createdDirs);
                validateFileNotExists(fullPath);

                String bizId = createFileWithStoragePrefix(ctx, fullPath, file, storagePathPrefix);
                fileIds.add(bizId);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload files", e);
        }
        return fileIds;
    }

    // ==================== 私有辅助方法 ====================

    private void ensureBaseDirectoryExists(VfsContext ctx, String basePath) {

        if (!"/".equals(basePath) && fileRepository.findByPath(basePath).isEmpty()) {
            directoryService.ensureDirs(ctx, basePath, new HashSet<>());
        }
    }

    private void ensureDirectoryExists(VfsContext ctx, String entryPath, Set<String> createdDirs) {

        if (!createdDirs.contains(entryPath) && fileRepository.findByPath(entryPath).isEmpty()) {
            directoryService.ensureDirs(ctx, entryPath, createdDirs);
        }
    }

    private void ensureParentDirectoryExists(VfsContext ctx, String entryPath, Set<String> createdDirs) {

        String parentPath = pathResolver.getParent(entryPath);
        if (parentPath != null && !"/".equals(parentPath) && !createdDirs.contains(parentPath)
                && fileRepository.findByPath(parentPath).isEmpty()) {
            directoryService.ensureDirs(ctx, parentPath, createdDirs);
        }
    }

    private void validateFileNotExists(String entryPath) {

        if (fileRepository.findByPath(entryPath).isPresent()) {
            throw new IllegalArgumentException("File already exists: " + entryPath);
        }
    }

    private String resolveStoragePathPrefix(String targetPath) {

        if (targetPath == null || "/".equals(targetPath) || targetPath.isEmpty()) {
            return null;
        }
        return targetPath.startsWith("/") ? targetPath.substring(1) : targetPath;
    }

    private String getRelativePath(String[] relativePaths, int index, MultipartFile file) {

        return relativePaths != null && index < relativePaths.length
               ? relativePaths[index]
               : file.getOriginalFilename();
    }

    private void ensureParentDirectoryForUpload(VfsContext ctx, String fullPath, Set<String> createdDirs) {

        String parentPath = pathResolver.getParent(fullPath);
        if (parentPath != null && !"/".equals(parentPath) && !createdDirs.contains(parentPath)) {
            if (fileRepository.findByPath(parentPath).isEmpty()) {
                directoryService.ensureDirs(ctx, parentPath, createdDirs);
            } else {
                createdDirs.add(parentPath);
            }
        }
    }

    private String createFileWithStoragePrefix(VfsContext ctx, String fullPath, MultipartFile file, String storagePathPrefix)
            throws IOException {

        return versionManager.createFileWithVersioning(ctx, fullPath, file.getInputStream(), file.getSize());
    }

}
