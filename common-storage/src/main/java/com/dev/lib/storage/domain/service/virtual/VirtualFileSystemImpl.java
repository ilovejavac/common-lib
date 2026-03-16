package com.dev.lib.storage.domain.service.virtual;

import com.dev.lib.storage.domain.model.VfsContext;
import com.dev.lib.storage.domain.model.VfsNode;
import com.dev.lib.storage.domain.service.virtual.core.VfsCoreDirectoryService;
import com.dev.lib.storage.domain.service.virtual.core.VfsFileService;
import com.dev.lib.storage.domain.service.virtual.storage.VfsFileStorageService;
import com.dev.lib.storage.domain.service.virtual.upload.VfsUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

/**
 * 虚拟文件系统实现
 * <p>
 * 作为入口服务，委托给扁平化的核心服务：
 * - VfsFileService：文件读写 + COW 版本管理
 * - VfsCoreDirectoryService：目录操作 + 搜索 + 通配符
 * - VfsUploadService：ZIP/多文件上传（保留）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VirtualFileSystemImpl {

    private final VfsFileService fileService;
    private final VfsCoreDirectoryService directoryService;
    private final VfsFileStorageService storageService;
    private final VfsUploadService uploadService;

    // ==================== 目录列表 ====================

    public List<VfsNode> listDirectory(VfsContext ctx, String path, Integer depth) {
        // depth 参数暂时忽略，新服务默认返回直接子节点
        return directoryService.list(ctx, path, ctx != null && ctx.isShowHidden());
    }

    // ==================== 文件读取 ====================

    public InputStream openFile(VfsContext ctx, String path) {
        return fileService.openStream(ctx, path);
    }

    public String readFile(VfsContext ctx, String path) {
        return fileService.readFile(ctx, path);
    }

    public List<String> readLines(VfsContext ctx, String path, int startLine, int lineCount) {
        return fileService.readLines(ctx, path, startLine, lineCount);
    }

    public byte[] readBytes(VfsContext ctx, String path, long offset, int limit) {
        return fileService.readBytes(ctx, path, offset, limit);
    }

    public long getFileSize(VfsContext ctx, String path) {
        return fileService.getSize(ctx, path);
    }

    public int getLineCount(VfsContext ctx, String path) {
        return fileService.getLineCount(ctx, path);
    }

    // ==================== 文件写入 ====================

    public void writeFile(VfsContext ctx, String path, String content) {
        fileService.write(ctx, path, content);
    }

    public void writeFile(VfsContext ctx, String path, InputStream inputStream) {
        fileService.writeStream(ctx, path, inputStream);
    }

    public void appendFile(VfsContext ctx, String path, String content) {
        fileService.append(ctx, path, content);
    }

    public void touchFile(VfsContext ctx, String path) {
        fileService.touch(ctx, path);
    }

    // ==================== 移动操作 ====================

    public void move(VfsContext ctx, String srcPath, String destPath) {
        directoryService.move(ctx, srcPath, destPath);
    }

    // ==================== 复制操作 ====================

    public void copy(VfsContext ctx, String srcPath, String destPath, boolean recursive) {
        directoryService.copy(ctx, srcPath, destPath, recursive);
    }

    // ==================== 删除操作 ====================

    public void delete(VfsContext ctx, String path, boolean recursive) {
        directoryService.delete(ctx, path, recursive);
    }

    // ==================== 目录创建 ====================

    public void createDirectory(VfsContext ctx, String path, boolean createParents) {
        if (createParents) {
            directoryService.mkdirp(ctx, path);
        } else {
            directoryService.mkdir(ctx, path);
        }
    }

    // ==================== 查找操作 ====================

    public List<VfsNode> findByName(VfsContext ctx, String basePath, String pattern, boolean recursive) {
        return directoryService.findByPattern(ctx, basePath, pattern);
    }

    public List<VfsNode> findByContent(VfsContext ctx, String basePath, String content, boolean recursive) {
        return directoryService.findByContent(ctx, basePath, content);
    }

    // ==================== 上传操作 ====================

    public List<String> uploadZip(VfsContext ctx, String path, InputStream zipStream) {
        return uploadService.uploadZip(ctx, path, zipStream);
    }

    public List<String> uploadFiles(VfsContext ctx, String targetPath, MultipartFile[] files, String[] relativePaths) {
        return uploadService.uploadFiles(ctx, targetPath, files, relativePaths);
    }

    public String uploadFile(VfsContext ctx, String path, InputStream inputStream, long size) {
        return uploadService.uploadFile(ctx, path, inputStream, size);
    }

    // ==================== 工具方法 ====================

    public boolean exists(VfsContext ctx, String path) {
        return fileService.exists(ctx, path);
    }

    public boolean isDirectory(VfsContext ctx, String path) {
        return directoryService.isDirectory(ctx, path);
    }

    // ==================== 内部接口（供 Command 使用）====================

    public String getStoragePath(VfsContext ctx, String virtualPath) {
        return fileService.getStoragePath(ctx, virtualPath);
    }

    public String replaceLinesByStoragePath(String storagePath, com.dev.lib.storage.Storage.LineTransformer transformer) throws java.io.IOException {
        return storageService.replaceLines(storagePath, transformer);
    }

    public String getPresignedUrlByStoragePath(String storagePath, int expireSeconds) {
        return storageService.getPresignedUrl(storagePath, expireSeconds);
    }
}
