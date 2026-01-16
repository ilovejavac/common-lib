package com.dev.lib.storage.domain.service.virtual.write;

import com.dev.lib.storage.domain.model.VfsContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * VFS 写入服务
 * 统一的写入操作入口，协调文件操作和文件系统操作
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VfsWriteService {

    private final VfsFileOperationService       fileOperationService;

    private final VfsFileSystemOperationService fileSystemOperationService;

    // ==================== 文件写入 ====================

    public void writeFile(VfsContext ctx, String path, String content) {

        fileOperationService.writeFile(ctx, path, content);
    }

    public void writeFile(VfsContext ctx, String path, InputStream inputStream) {

        fileOperationService.writeFile(ctx, path, inputStream);
    }

    public void appendFile(VfsContext ctx, String path, String content) {

        fileOperationService.appendFile(ctx, path, content);
    }

    public void touchFile(VfsContext ctx, String path) {

        fileOperationService.touchFile(ctx, path);
    }

    // ==================== 移动操作 ====================

    public void move(VfsContext ctx, String srcPath, String destPath) {

        fileSystemOperationService.move(ctx, srcPath, destPath);
    }

    // ==================== 复制操作 ====================

    public void copy(VfsContext ctx, String srcPath, String destPath, boolean recursive) {

        fileSystemOperationService.copy(ctx, srcPath, destPath, recursive);
    }

    // ==================== 删除操作 ====================

    public void delete(VfsContext ctx, String path, boolean recursive) {

        fileSystemOperationService.delete(ctx, path, recursive);
    }

    // ==================== 目录创建 ====================

    public void createDirectory(VfsContext ctx, String path, boolean createParents) {

        fileOperationService.createDirectory(ctx, path, createParents);
    }

}
