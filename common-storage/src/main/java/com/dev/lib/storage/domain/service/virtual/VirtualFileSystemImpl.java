package com.dev.lib.storage.domain.service.virtual;

import com.dev.lib.storage.data.SysFile;
import com.dev.lib.storage.domain.model.VfsContext;
import com.dev.lib.storage.domain.model.VfsNode;
import com.dev.lib.storage.domain.service.StorageService;
import com.dev.lib.storage.domain.service.VirtualFileSystem;
import com.dev.lib.storage.domain.service.virtual.path.VfsPathResolver;
import com.dev.lib.storage.domain.service.virtual.read.VfsReadService;
import com.dev.lib.storage.domain.service.virtual.repository.VfsFileRepository;
import com.dev.lib.storage.domain.service.virtual.search.VfsSearchService;
import com.dev.lib.storage.domain.service.virtual.storage.VfsFileStorageService;
import com.dev.lib.storage.domain.service.virtual.upload.VfsUploadService;
import com.dev.lib.storage.domain.service.virtual.write.VfsWriteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

/**
 * 虚拟文件系统实现
 * 作为入口服务，协调各个领域服务完成 VFS 操作
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VirtualFileSystemImpl implements VirtualFileSystem {

    private final VfsFileRepository fileRepository;

    private final VfsPathResolver pathResolver;

    private final VfsFileStorageService storageService;

    private final VfsReadService readService;

    private final VfsWriteService writeService;

    private final VfsUploadService uploadService;

    private final VfsSearchService searchService;

    // ==================== 目录列表 ====================

    @Override
    public List<VfsNode> listDirectory(VfsContext ctx, String path, Integer depth) {

        return readService.listDirectory(ctx, path, depth);
    }

    // ==================== 文件读取 ====================

    @Override
    public InputStream openFile(VfsContext ctx, String path) {

        return readService.openFile(ctx, path);
    }

    @Override
    public String readFile(VfsContext ctx, String path) {

        return readService.readFile(ctx, path);
    }

    @Override
    public List<String> readLines(VfsContext ctx, String path, int startLine, int lineCount) {

        return readService.readLines(ctx, path, startLine, lineCount);
    }

    @Override
    public byte[] readBytes(VfsContext ctx, String path, long offset, int limit) {

        return readService.readBytes(ctx, path, offset, limit);
    }

    @Override
    public long getFileSize(VfsContext ctx, String path) {

        return readService.getFileSize(ctx, path);
    }

    @Override
    public int getLineCount(VfsContext ctx, String path) {

        return readService.getLineCount(ctx, path);
    }

    // ==================== 文件写入 ====================

    @Override
    public void writeFile(VfsContext ctx, String path, String content) {

        writeService.writeFile(ctx, path, content);
    }

    @Override
    public void writeFile(VfsContext ctx, String path, InputStream inputStream) {

        writeService.writeFile(ctx, path, inputStream);
    }

    @Override
    public void appendFile(VfsContext ctx, String path, String content) {

        writeService.appendFile(ctx, path, content);
    }

    @Override
    public void touchFile(VfsContext ctx, String path) {

        writeService.touchFile(ctx, path);
    }

    // ==================== 移动操作 ====================

    @Override
    public void move(VfsContext ctx, String srcPath, String destPath) {

        writeService.move(ctx, srcPath, destPath);
    }

    // ==================== 复制操作 ====================

    @Override
    public void copy(VfsContext ctx, String srcPath, String destPath, boolean recursive) {

        writeService.copy(ctx, srcPath, destPath, recursive);
    }

    // ==================== 删除操作 ====================

    @Override
    public void delete(VfsContext ctx, String path, boolean recursive) {

        writeService.delete(ctx, path, recursive);
    }

    // ==================== 目录创建 ====================

    @Override
    public void createDirectory(VfsContext ctx, String path, boolean createParents) {

        writeService.createDirectory(ctx, path, createParents);
    }

    // ==================== 查找操作 ====================

    @Override
    public List<VfsNode> findByName(VfsContext ctx, String basePath, String pattern, boolean recursive) {

        return searchService.findByName(ctx, basePath, pattern, recursive);
    }

    @Override
    public List<VfsNode> findByContent(VfsContext ctx, String basePath, String content, boolean recursive) {

        return searchService.findByContent(ctx, basePath, content, recursive);
    }

    // ==================== 上传操作 ====================

    @Override
    public List<String> uploadZip(VfsContext ctx, String path, InputStream zipStream) {

        return uploadService.uploadZip(ctx, path, zipStream);
    }

    @Override
    public List<String> uploadFiles(VfsContext ctx, String targetPath, MultipartFile[] files, String[] relativePaths) {

        return uploadService.uploadFiles(ctx, targetPath, files, relativePaths);
    }

    // ==================== 工具方法 ====================

    @Override
    public boolean exists(VfsContext ctx, String path) {

        return readService.exists(ctx, path);
    }

    @Override
    public boolean isDirectory(VfsContext ctx, String path) {

        return readService.isDirectory(ctx, path);
    }

    // ==================== 内部接口（供 Command 使用）====================

    @Override
    public String getStoragePath(VfsContext ctx, String virtualPath) {

        String fullPath = pathResolver.resolve(ctx, virtualPath);
        return fileRepository.findByPath(fullPath).map(SysFile::getStoragePath).orElse(null);
    }

    @Override
    public StorageService getStorageService() {

        return storageService.getStorageService();
    }

}
