package com.dev.lib.storage.domain.service.virtual.read;

import com.dev.lib.storage.data.SysFile;
import com.dev.lib.storage.domain.model.VfsContext;
import com.dev.lib.storage.domain.model.VfsNode;
import com.dev.lib.storage.domain.service.virtual.node.VfsNodeFactory;
import com.dev.lib.storage.domain.service.virtual.path.VfsPathResolver;
import com.dev.lib.storage.domain.service.virtual.repository.VfsFileRepository;
import com.dev.lib.storage.domain.service.virtual.storage.VfsFileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * VFS 读取服务
 * 负责文件和目录的读取操作
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VfsReadService {

    private static final int MAX_READ_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    private final VfsFileRepository     fileRepository;

    private final VfsPathResolver       pathResolver;

    private final VfsNodeFactory        nodeFactory;

    private final VfsFileStorageService storageService;

    // ==================== 目录列表 ====================

    /**
     * 列出目录内容
     */
    public List<VfsNode> listDirectory(VfsContext ctx, String path, Integer depth) {

        if (depth == null || depth < 1) depth = 1;
        if (depth > 3) depth = 3;

        String fullPath = pathResolver.resolve(ctx, path);

        if ("/".equals(fullPath) || fullPath.isEmpty()) {
            return nodeFactory.buildNodes(ctx, fileRepository.findChildren("/"), depth);
        }

        log.info("fullPath {}", fullPath);
        SysFile target = fileRepository.findByPath(fullPath)
                .orElseThrow(() -> new IllegalArgumentException("Path not found: " + fullPath));

        if (!Boolean.TRUE.equals(target.getIsDirectory())) {
            return List.of(nodeFactory.toNode(ctx, target, 0));
        }

        return nodeFactory.buildNodes(ctx, fileRepository.findChildren(fullPath), depth);
    }

    // ==================== 文件读取 ====================

    /**
     * 打开文件输入流
     * <p>注意：调用者必须负责关闭返回的 InputStream，建议使用 try-with-resources。
     * 如果需要自动资源管理，请使用 readFile() 或 readBytes() 方法。
     *
     * @param ctx VFS 上下文
     * @param path 文件路径
     * @return 文件输入流，必须由调用者关闭
     * @throws IllegalArgumentException 如果文件不存在或是目录
     * @throws RuntimeException 如果读取失败
     */
    public InputStream openFile(VfsContext ctx, String path) {

        String fullPath = pathResolver.resolve(ctx, path);
        SysFile file = fileRepository.findByPath(fullPath)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + path));

        if (Boolean.TRUE.equals(file.getIsDirectory())) {
            throw new IllegalArgumentException("Cannot read directory: " + path);
        }

        try {
            return storageService.download(file.getStoragePath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file", e);
        }
    }

    /**
     * 读取文件内容为字符串
     */
    public String readFile(VfsContext ctx, String path) {

        String fullPath = pathResolver.resolve(ctx, path);
        SysFile file = fileRepository.findByPath(fullPath)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + path));

        if (file.getSize() != null && file.getSize() == 0) {
            return "";
        }
        if (file.getSize() != null && file.getSize() > MAX_READ_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "File too large (" + file.getSize() + " bytes). Use readLines() instead.");
        }

        try (InputStream is = openFile(ctx, path)) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file", e);
        }
    }

    /**
     * 按行读取文件
     */
    public List<String> readLines(VfsContext ctx, String path, int startLine, int lineCount) {

        if (startLine < 1) {
            throw new IllegalArgumentException("Start line must be >= 1");
        }

        List<String> result = new ArrayList<>();
        try (InputStream is = openFile(ctx, path);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

            for (int i = 1; i < startLine; i++) {
                if (reader.readLine() == null) return result;
            }

            String line;
            int    count = 0;
            while ((line = reader.readLine()) != null) {
                result.add(line);
                count++;
                if (lineCount != -1 && count >= lineCount) break;
            }
            return result;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read lines from file", e);
        }
    }

    /**
     * 读取文件字节数组
     */
    public byte[] readBytes(VfsContext ctx, String path, long offset, int limit) {

        if (offset < 0) {
            throw new IllegalArgumentException("Offset must be >= 0");
        }

        String fullPath = pathResolver.resolve(ctx, path);
        SysFile file = fileRepository.findByPath(fullPath)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + path));

        if (Boolean.TRUE.equals(file.getIsDirectory())) {
            throw new IllegalArgumentException("Cannot read directory: " + path);
        }

        try (InputStream is = openFile(ctx, path)) {
            long skipped = 0;
            while (skipped < offset) {
                long s = is.skip(offset - skipped);
                if (s == 0) break;
                skipped += s;
            }

            if (limit == -1) {
                return is.readAllBytes();
            }

            byte[] result    = new byte[limit];
            int    totalRead = 0;
            while (totalRead < limit) {
                int read = is.read(result, totalRead, limit - totalRead);
                if (read == -1) break;
                totalRead += read;
            }

            if (totalRead < limit) {
                byte[] trimmed = new byte[totalRead];
                System.arraycopy(result, 0, trimmed, 0, totalRead);
                return trimmed;
            }
            return result;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read bytes from file", e);
        }
    }

    /**
     * 获取文件大小
     */
    public long getFileSize(VfsContext ctx, String path) {

        String fullPath = pathResolver.resolve(ctx, path);
        SysFile file = fileRepository.findByPath(fullPath)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + path));

        if (Boolean.TRUE.equals(file.getIsDirectory())) {
            throw new IllegalArgumentException("Cannot get size of directory: " + path);
        }

        return file.getSize() != null ? file.getSize() : 0;
    }

    /**
     * 获取文件行数
     */
    public int getLineCount(VfsContext ctx, String path) {

        try (InputStream is = openFile(ctx, path);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            int count = 0;
            while (reader.readLine() != null) count++;
            return count;
        } catch (IOException e) {
            throw new RuntimeException("Failed to count lines in file", e);
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 检查路径是否存在
     */
    public boolean exists(VfsContext ctx, String path) {

        return fileRepository.findByPath(pathResolver.resolve(ctx, path)).isPresent();
    }

    /**
     * 检查路径是否为目录
     */
    public boolean isDirectory(VfsContext ctx, String path) {

        return fileRepository.findByPath(pathResolver.resolve(ctx, path))
                .map(f -> Boolean.TRUE.equals(f.getIsDirectory()))
                .orElse(false);
    }

}
