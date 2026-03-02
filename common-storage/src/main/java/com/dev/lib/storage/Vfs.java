package com.dev.lib.storage;

import com.dev.lib.storage.domain.model.VfsContext;
import com.dev.lib.storage.domain.model.VfsNode;
import com.dev.lib.storage.domain.service.virtual.VirtualFileSystemImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;

/**
 * VFS 静态入口，和 Storage 保持一致的使用方式。
 */
@Component
@RequiredArgsConstructor
public class Vfs {

    private final VirtualFileSystemImpl delegate;

    @Value("${spring.application.name:unknown-service}")
    private String applicationName;

    private static Vfs instance;
    private static String defaultServiceName = "unknown-service";

    @jakarta.annotation.PostConstruct
    public void init() {

        instance = this;
        defaultServiceName = (applicationName == null || applicationName.isBlank())
                             ? "unknown-service"
                             : applicationName;
    }

    private static VirtualFileSystemImpl d() {

        if (instance == null) {
            throw new IllegalStateException("Vfs is not initialized");
        }
        return instance.delegate;
    }

    // ==================== 链式入口（对齐 Storage） ====================

    public static ContextBuilder root(String root) {
        return context(VfsContext.of(root));
    }

    public static ContextBuilder root() {
        return root(null);
    }

    public static ContextBuilder context(VfsContext context) {
        return new ContextBuilder(context);
    }

    public static class ContextBuilder {

        private final VfsContext ctx;

        ContextBuilder(VfsContext ctx) {
            this.ctx = ctx == null ? VfsContext.of(null) : ctx;
            if (this.ctx.getServiceName() == null || this.ctx.getServiceName().isBlank()) {
                this.ctx.setServiceName(defaultServiceName);
            }
        }

        public ContextBuilder service(String serviceName) {
            ctx.setServiceName(serviceName);
            return this;
        }

        public ContextBuilder temporary(boolean temporary) {
            ctx.setTemporary(temporary);
            return this;
        }

        public ContextBuilder expireAt(LocalDateTime expirationAt) {
            ctx.setExpirationAt(expirationAt);
            return this;
        }

        public ContextBuilder showHidden(boolean showHidden) {
            ctx.setShowHidden(showHidden);
            return this;
        }

        public FileBuilder file(String path) {
            return new FileBuilder(ctx, path);
        }

        public List<VfsNode> ls(String path, Integer depth) {
            return Vfs.listDirectory(ctx, path, depth);
        }

        public List<VfsNode> findByName(String basePath, String pattern, boolean recursive) {
            return Vfs.findByName(ctx, basePath, pattern, recursive);
        }

        public List<VfsNode> findByContent(String basePath, String content, boolean recursive) {
            return Vfs.findByContent(ctx, basePath, content, recursive);
        }

        public void mkdir(String path, boolean createParents) {
            Vfs.createDirectory(ctx, path, createParents);
        }

        public void rm(String path, boolean recursive) {
            Vfs.delete(ctx, path, recursive);
        }

        public void mv(String srcPath, String destPath) {
            Vfs.move(ctx, srcPath, destPath);
        }

        public void cp(String srcPath, String destPath, boolean recursive) {
            Vfs.copy(ctx, srcPath, destPath, recursive);
        }

        public List<String> uploadZip(String path, InputStream zipStream) {
            return Vfs.uploadZip(ctx, path, zipStream);
        }

        public List<String> uploadFiles(String targetPath, MultipartFile[] files, String[] relativePaths) {
            return Vfs.uploadFiles(ctx, targetPath, files, relativePaths);
        }

        public String uploadFile(String path, InputStream inputStream, long size) {
            return Vfs.uploadFile(ctx, path, inputStream, size);
        }

        public VfsContext raw() {
            return ctx;
        }
    }

    public static class FileBuilder {

        private final VfsContext ctx;
        private final String path;

        FileBuilder(VfsContext ctx, String path) {
            if (path == null || path.isBlank()) {
                throw new IllegalArgumentException("path must not be empty");
            }
            this.ctx = ctx;
            this.path = path;
        }

        public InputStream open() {
            return Vfs.openFile(ctx, path);
        }

        public String read() {
            return Vfs.readFile(ctx, path);
        }

        public List<String> readLines(int startLine, int lineCount) {
            return Vfs.readLines(ctx, path, startLine, lineCount);
        }

        public byte[] readBytes(long offset, int limit) {
            return Vfs.readBytes(ctx, path, offset, limit);
        }

        public long size() {
            return Vfs.getFileSize(ctx, path);
        }

        public int lineCount() {
            return Vfs.getLineCount(ctx, path);
        }

        public boolean exists() {
            return Vfs.exists(ctx, path);
        }

        public boolean isDirectory() {
            return Vfs.isDirectory(ctx, path);
        }

        public void write(String content) {
            Vfs.writeFile(ctx, path, content);
        }

        public void write(InputStream inputStream) {
            Vfs.writeFile(ctx, path, inputStream);
        }

        public void append(String content) {
            Vfs.appendFile(ctx, path, content);
        }

        public void touch() {
            Vfs.touchFile(ctx, path);
        }

        public void delete(boolean recursive) {
            Vfs.delete(ctx, path, recursive);
        }

        public void moveTo(String destPath) {
            Vfs.move(ctx, path, destPath);
        }

        public void copyTo(String destPath, boolean recursive) {
            Vfs.copy(ctx, path, destPath, recursive);
        }

        public String upload(InputStream inputStream, long size) {
            return Vfs.uploadFile(ctx, path, inputStream, size);
        }

        public String storagePath() {
            return Vfs.getStoragePath(ctx, path);
        }

        public String presignedUrl(int expireSeconds) {
            String storagePath = storagePath();
            if (storagePath == null) {
                return null;
            }
            return Vfs.getPresignedUrlByStoragePath(storagePath, expireSeconds);
        }
    }

    public static List<VfsNode> listDirectory(VfsContext ctx, String path, Integer depth) {
        return d().listDirectory(ctx, path, depth);
    }

    public static InputStream openFile(VfsContext ctx, String path) {
        return d().openFile(ctx, path);
    }

    public static String readFile(VfsContext ctx, String path) {
        return d().readFile(ctx, path);
    }

    public static List<String> readLines(VfsContext ctx, String path, int startLine, int lineCount) {
        return d().readLines(ctx, path, startLine, lineCount);
    }

    public static byte[] readBytes(VfsContext ctx, String path, long offset, int limit) {
        return d().readBytes(ctx, path, offset, limit);
    }

    public static long getFileSize(VfsContext ctx, String path) {
        return d().getFileSize(ctx, path);
    }

    public static int getLineCount(VfsContext ctx, String path) {
        return d().getLineCount(ctx, path);
    }

    public static boolean exists(VfsContext ctx, String path) {
        return d().exists(ctx, path);
    }

    public static boolean isDirectory(VfsContext ctx, String path) {
        return d().isDirectory(ctx, path);
    }

    public static void writeFile(VfsContext ctx, String path, String content) {
        d().writeFile(ctx, path, content);
    }

    public static void writeFile(VfsContext ctx, String path, InputStream inputStream) {
        d().writeFile(ctx, path, inputStream);
    }

    public static void appendFile(VfsContext ctx, String path, String content) {
        d().appendFile(ctx, path, content);
    }

    public static void touchFile(VfsContext ctx, String path) {
        d().touchFile(ctx, path);
    }

    public static void copy(VfsContext ctx, String srcPath, String destPath, boolean recursive) {
        d().copy(ctx, srcPath, destPath, recursive);
    }

    public static void move(VfsContext ctx, String srcPath, String destPath) {
        d().move(ctx, srcPath, destPath);
    }

    public static void delete(VfsContext ctx, String path, boolean recursive) {
        d().delete(ctx, path, recursive);
    }

    public static void createDirectory(VfsContext ctx, String path, boolean createParents) {
        d().createDirectory(ctx, path, createParents);
    }

    public static List<VfsNode> findByName(VfsContext ctx, String basePath, String pattern, boolean recursive) {
        return d().findByName(ctx, basePath, pattern, recursive);
    }

    public static List<VfsNode> findByContent(VfsContext ctx, String basePath, String content, boolean recursive) {
        return d().findByContent(ctx, basePath, content, recursive);
    }

    public static List<String> uploadZip(VfsContext ctx, String path, InputStream zipStream) {
        return d().uploadZip(ctx, path, zipStream);
    }

    public static List<String> uploadFiles(VfsContext ctx, String targetPath, MultipartFile[] files, String[] relativePaths) {
        return d().uploadFiles(ctx, targetPath, files, relativePaths);
    }

    public static String uploadFile(VfsContext ctx, String path, InputStream inputStream, long size) {
        return d().uploadFile(ctx, path, inputStream, size);
    }

    public static String getStoragePath(VfsContext ctx, String virtualPath) {
        return d().getStoragePath(ctx, virtualPath);
    }

    public static String replaceLinesByStoragePath(String storagePath, Storage.LineTransformer transformer) throws java.io.IOException {
        return d().replaceLinesByStoragePath(storagePath, transformer);
    }

    public static String getPresignedUrlByStoragePath(String storagePath, int expireSeconds) {
        return d().getPresignedUrlByStoragePath(storagePath, expireSeconds);
    }

}
