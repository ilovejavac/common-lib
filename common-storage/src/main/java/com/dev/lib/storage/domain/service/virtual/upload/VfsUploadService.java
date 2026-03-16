package com.dev.lib.storage.domain.service.virtual.upload;

import com.dev.lib.storage.data.SysFile;
import com.dev.lib.storage.domain.model.VfsContext;
import com.dev.lib.storage.domain.service.virtual.core.VfsCoreDirectoryService;
import com.dev.lib.storage.domain.service.virtual.core.VfsFileService;
import com.dev.lib.storage.domain.service.virtual.path.VfsPathResolver;
import com.dev.lib.storage.domain.service.virtual.repository.VfsFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * VFS 上传服务
 * 负责 ZIP 文件和多文件的上传操作
 * <p>
 * 已迁移至新核心服务：
 * - 目录创建委托给 VfsCoreDirectoryService.mkdirp()
 * - 文件写入委托给 VfsFileService.writeStream()
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VfsUploadService {

    private final VfsFileRepository fileRepository;
    private final VfsPathResolver pathResolver;
    private final VfsCoreDirectoryService directoryService;
    private final VfsFileService fileService;

    // ==================== ZIP 上传 ====================

    /**
     * 上传并解压 ZIP 文件（如果文件存在则覆盖）
     */
    @Transactional(rollbackFor = Exception.class)
    public List<String> uploadZip(VfsContext ctx, String path, InputStream zipStream) {
        String basePath = pathResolver.resolve(ctx, path);

        if (!"/".equals(basePath) && fileRepository.findByPath(basePath).isEmpty()) {
            directoryService.mkdirp(ctx, basePath);
        }

        List<String> fileIds = new ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(zipStream)) {
            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {
                String entryPath = pathResolver.normalize(basePath + "/" + entry.getName());

                if (entry.isDirectory()) {
                    directoryService.mkdirp(ctx, entryPath);
                } else {
                    // writeStream 会自动创建父目录和处理 COW
                    fileService.writeStream(ctx, entryPath, zis);
                    String bizId = resolveBizId(entryPath);
                    if (bizId != null) {
                        fileIds.add(bizId);
                    }
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
     * 上传单个文件（如果存在则覆盖）
     */
    @Transactional(rollbackFor = Exception.class)
    public String uploadFile(VfsContext ctx, String path, InputStream inputStream, long size) {
        String fullPath = pathResolver.resolve(ctx, path);

        // writeStream 会自动创建父目录和处理 COW
        fileService.writeStream(ctx, fullPath, inputStream);

        return resolveBizId(fullPath);
    }

    // ==================== 多文件上传 ====================

    /**
     * 上传多个文件（基于 MultipartFile，如果文件存在则覆盖）
     */
    @Transactional(rollbackFor = Exception.class)
    public List<String> uploadFiles(VfsContext ctx, String targetPath, MultipartFile[] files, String[] relativePaths) {
        if (files == null || files.length == 0) {
            throw new IllegalArgumentException("Files array must not be null or empty");
        }
        if (relativePaths != null && relativePaths.length != files.length) {
            throw new IllegalArgumentException(
                    "relativePaths length must match files length: expected " + files.length + ", got " + relativePaths.length);
        }

        String basePath = pathResolver.resolve(ctx, targetPath);

        if (!"/".equals(basePath) && fileRepository.findByPath(basePath).isEmpty()) {
            directoryService.mkdirp(ctx, basePath);
        }

        List<String> fileIds = new ArrayList<>();

        try {
            for (int i = 0; i < files.length; i++) {
                MultipartFile file = files[i];
                String relativePath = getRelativePath(relativePaths, i, file);

                if (relativePath == null || relativePath.isEmpty()) continue;

                String fullPath = pathResolver.normalize(basePath + "/" + relativePath);

                try (InputStream inputStream = file.getInputStream()) {
                    // writeStream 会自动创建父目录和处理 COW
                    fileService.writeStream(ctx, fullPath, inputStream);
                    String bizId = resolveBizId(fullPath);
                    if (bizId != null) {
                        fileIds.add(bizId);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload files", e);
        }
        return fileIds;
    }

    // ==================== 私有辅助方法 ====================

    private String getRelativePath(String[] relativePaths, int index, MultipartFile file) {
        return relativePaths != null && index < relativePaths.length
               ? relativePaths[index]
               : file.getOriginalFilename();
    }

    private String resolveBizId(String fullPath) {
        return fileRepository.findByPath(fullPath)
                .map(SysFile::getBizId)
                .orElse(null);
    }
}
