package com.dev.lib.storage.domain.service.virtual.core;

import com.dev.lib.entity.id.IDWorker;
import com.dev.lib.storage.config.AppStorageProperties;
import com.dev.lib.storage.data.SysFile;
import com.dev.lib.storage.domain.model.VfsContext;
import com.dev.lib.storage.domain.model.VfsStat;
import com.dev.lib.storage.domain.service.virtual.StorageServiceNameProvider;
import com.dev.lib.storage.domain.service.virtual.path.VfsPathResolver;
import com.dev.lib.storage.domain.service.virtual.repository.VfsFileRepository;
import com.dev.lib.storage.domain.service.virtual.storage.VfsFileStorageService;
import com.dev.lib.storage.domain.service.write.SysFileCowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * VFS 文件服务 - 扁平化核心服务
 * <p>
 * 职责：
 * - 文件读取（流式处理，不加载到内存）
 * - 文件写入（流式处理 + COW 版本管理）
 * - 文件信息查询
 * <p>
 * 设计原则：
 * - 所有操作基于 InputStream，避免全量加载
 * - COW 逻辑委托给 SysFileCowService
 * - 嵌套深度控制在 3 层以内
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VfsFileService {

    private final VfsFileRepository fileRepository;
    private final VfsFileStorageService storageService;
    private final VfsPathResolver pathResolver;
    private final StorageServiceNameProvider serviceNameProvider;
    private final SysFileCowService cowService;
    private final AppStorageProperties storageProperties;

    @Lazy
    private final VfsCoreDirectoryService directoryService;

    // ========== 流式读取 ==========

    /**
     * 打开文件流 - 核心读取方法，不加载到内存
     */
    @Transactional(readOnly = true)
    public InputStream openStream(VfsContext ctx, String virtualPath) {
        String fullPath = pathResolver.resolve(ctx, virtualPath);

        SysFile file = fileRepository.findByPath(fullPath)
            .orElseThrow(() -> new IllegalArgumentException("File not found: " + virtualPath));

        if (Boolean.TRUE.equals(file.getIsDirectory())) {
            throw new IllegalArgumentException("Cannot read directory: " + virtualPath);
        }

        try {
            return storageService.download(file.getStoragePath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to open file: " + virtualPath, e);
        }
    }

    /**
     * 读取指定行范围（流式处理）
     */
    public List<String> readLines(VfsContext ctx, String virtualPath, int startLine, int lineCount) {
        try (InputStream input = openStream(ctx, virtualPath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {

            for (int i = 1; i < startLine; i++) {
                if (reader.readLine() == null) return List.of();
            }

            List<String> lines = new ArrayList<>(lineCount);
            for (int i = 0; i < lineCount; i++) {
                String line = reader.readLine();
                if (line == null) break;
                lines.add(line);
            }
            return lines;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read lines: " + virtualPath, e);
        }
    }

    /**
     * 读取指定字节范围（流式处理）
     */
    public byte[] readBytes(VfsContext ctx, String virtualPath, long offset, int limit) {
        try (InputStream input = openStream(ctx, virtualPath)) {
            long skipped = 0;
            while (skipped < offset) {
                long s = input.skip(offset - skipped);
                if (s == 0) break;
                skipped += s;
            }

            if (limit == -1) {
                return input.readAllBytes();
            }

            byte[] result = new byte[limit];
            int totalRead = 0;
            while (totalRead < limit) {
                int read = input.read(result, totalRead, limit - totalRead);
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
            throw new RuntimeException("Failed to read bytes: " + virtualPath, e);
        }
    }

    /**
     * 读取完整文件内容为字符串（小文件使用）
     */
    public String readFile(VfsContext ctx, String virtualPath) {
        try (InputStream input = openStream(ctx, virtualPath)) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file: " + virtualPath, e);
        }
    }

    // ========== 流式写入（COW 版本管理） ==========

    /**
     * 写入文件流 - 核心写入方法，支持 COW
     */
    @Transactional
    public void writeStream(VfsContext ctx, String virtualPath, InputStream input) {
        String fullPath = pathResolver.resolve(ctx, virtualPath);

        // 确保父目录存在
        ensureParentExists(ctx, fullPath);

        SysFile file = fileRepository.findByPathForUpdate(fullPath).orElse(null);

        if (file == null) {
            createNewFile(ctx, fullPath, input);
        } else {
            updateFileWithCOW(ctx, file, fullPath, input);
        }
    }

    /**
     * 写入字符串内容
     */
    @Transactional
    public void write(VfsContext ctx, String virtualPath, String content) {
        byte[] bytes = content == null ? new byte[0] : content.getBytes(StandardCharsets.UTF_8);
        try (InputStream input = new ByteArrayInputStream(bytes)) {
            writeStream(ctx, virtualPath, input);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write file: " + virtualPath, e);
        }
    }

    /**
     * 追加内容
     */
    @Transactional
    public void append(VfsContext ctx, String virtualPath, String content) {
        String fullPath = pathResolver.resolve(ctx, virtualPath);

        // 确保父目录存在
        ensureParentExists(ctx, fullPath);

        SysFile file = fileRepository.findByPathForUpdate(fullPath).orElse(null);
        byte[] contentBytes = content == null ? new byte[0] : content.getBytes(StandardCharsets.UTF_8);

        if (file == null) {
            // 文件不存在，创建新文件
            try (InputStream input = new ByteArrayInputStream(contentBytes)) {
                createNewFile(ctx, fullPath, input);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create file: " + virtualPath, e);
            }
        } else {
            // 文件存在，追加内容
            if (Boolean.TRUE.equals(file.getIsDirectory())) {
                throw new IllegalArgumentException("Cannot append to directory: " + virtualPath);
            }
            try {
                String fileName = pathResolver.getName(fullPath);
                cowService.appendWithCOW(file, contentBytes, fileName);
                applyTemporaryMetadata(file, ctx, false);
                fileRepository.save(file);
            } catch (IOException e) {
                throw new RuntimeException("Failed to append file: " + virtualPath, e);
            }
        }
    }

    /**
     * 创建空文件或更新时间戳
     */
    @Transactional
    public void touch(VfsContext ctx, String virtualPath) {
        String fullPath = pathResolver.resolve(ctx, virtualPath);

        SysFile file = fileRepository.findByPathForUpdate(fullPath).orElse(null);

        if (file == null) {
            ensureParentExists(ctx, fullPath);
            try (InputStream input = InputStream.nullInputStream()) {
                createNewFile(ctx, fullPath, input);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create empty file: " + virtualPath, e);
            }
        } else {
            if (Boolean.TRUE.equals(file.getIsDirectory())) {
                throw new IllegalArgumentException("Cannot touch directory: " + virtualPath);
            }
            fileRepository.save(file);
        }
    }

    // ========== 文件信息 ==========

    /**
     * 获取文件详细信息
     */
    @Transactional(readOnly = true)
    public VfsStat stat(VfsContext ctx, String virtualPath) {
        String fullPath = pathResolver.resolve(ctx, virtualPath);
        SysFile file = fileRepository.findByPath(fullPath)
            .orElseThrow(() -> new IllegalArgumentException("File not found: " + virtualPath));

        return VfsStat.builder()
            .path(file.getVirtualPath())
            .size(file.getSize())
            .isDirectory(file.getIsDirectory())
            .createTime(file.getCreatedAt())
            .updateTime(file.getUpdatedAt())
            .build();
    }

    /**
     * 检查文件是否存在
     */
    @Transactional(readOnly = true)
    public boolean exists(VfsContext ctx, String virtualPath) {
        String fullPath = pathResolver.resolve(ctx, virtualPath);
        return fileRepository.findByPath(fullPath).isPresent();
    }

    /**
     * 获取文件大小
     */
    @Transactional(readOnly = true)
    public long getSize(VfsContext ctx, String virtualPath) {
        String fullPath = pathResolver.resolve(ctx, virtualPath);
        return fileRepository.findByPath(fullPath)
            .map(f -> f.getSize() != null ? f.getSize() : 0L)
            .orElse(0L);
    }

    /**
     * 获取行数（流式统计）
     */
    public int getLineCount(VfsContext ctx, String virtualPath) {
        try (InputStream input = openStream(ctx, virtualPath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            int count = 0;
            while (reader.readLine() != null) {
                count++;
            }
            return count;
        } catch (IOException e) {
            throw new RuntimeException("Failed to count lines: " + virtualPath, e);
        }
    }

    /**
     * 获取存储路径
     */
    @Transactional(readOnly = true)
    public String getStoragePath(VfsContext ctx, String virtualPath) {
        String fullPath = pathResolver.resolve(ctx, virtualPath);
        return fileRepository.findByPath(fullPath)
            .map(SysFile::getStoragePath)
            .orElse(null);
    }

    /**
     * 删除文件
     */
    @Transactional
    public void delete(VfsContext ctx, String virtualPath) {
        String fullPath = pathResolver.resolve(ctx, virtualPath);

        SysFile file = fileRepository.findByPath(fullPath)
            .orElseThrow(() -> new IllegalArgumentException("File not found: " + virtualPath));

        if (Boolean.TRUE.equals(file.getIsDirectory())) {
            throw new IllegalArgumentException("Cannot delete directory with file service, use directory service");
        }

        // 删除存储层文件
        List<String> allPaths = storageService.collectStoragePaths(file);
        storageService.deleteAll(allPaths);

        fileRepository.delete(file);
    }

    // ========== 私有辅助方法 ==========

    private void ensureParentExists(VfsContext ctx, String fullPath) {
        String parentPath = pathResolver.getParent(fullPath);
        if (parentPath != null && !"/".equals(parentPath) && fileRepository.findByPath(parentPath).isEmpty()) {
            directoryService.mkdirp(ctx, parentPath);
        }
    }

    private void createNewFile(VfsContext ctx, String fullPath, InputStream input) {
        String fileName = pathResolver.getName(fullPath);
        String extension = pathResolver.getExtension(fileName);

        try {
            String storagePath = storageService.upload(input, fileName, null);

            SysFile file = new SysFile();
            file.setBizId(IDWorker.newId());
            file.setVirtualPath(fullPath);
            file.setParentPath(pathResolver.getParent(fullPath));
            file.setIsDirectory(false);
            file.setOriginalName(fileName);
            file.setStorageName(fileName);
            file.setStoragePath(storagePath);
            file.setExtension(extension);
            file.setSize(-1L); // 大小由存储层设置
            file.setStorageType(storageService.getStorageProperties().getType());
            file.setHidden(fileName.startsWith("."));
            file.setServiceName(serviceNameProvider.resolve(ctx));
            applyTemporaryMetadata(file, ctx, true);
            fileRepository.save(file);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create file: " + fullPath, e);
        }
    }

    private void updateFileWithCOW(VfsContext ctx, SysFile file, String fullPath, InputStream input) {
        if (Boolean.TRUE.equals(file.getIsDirectory())) {
            throw new IllegalArgumentException("Cannot write to directory: " + fullPath);
        }

        String fileName = pathResolver.getName(fullPath);

        try {
            cowService.writeWithCOW(file, input, -1, fileName);
            if (file.getServiceName() == null || file.getServiceName().isBlank()) {
                file.setServiceName(serviceNameProvider.resolve(ctx));
            }
            applyTemporaryMetadata(file, ctx, false);
            fileRepository.save(file);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write file: " + fullPath, e);
        }
    }

    private void applyTemporaryMetadata(SysFile file, VfsContext ctx, boolean isCreate) {
        if (ctx == null) return;

        if (isCreate) {
            boolean temporary = Boolean.TRUE.equals(ctx.getTemporary());
            file.setTemporary(temporary);
            if (temporary) {
                file.setExpirationAt(resolveExpirationAt(ctx));
            }
        } else {
            if (ctx.getTemporary() == null) return;
            if (Boolean.TRUE.equals(ctx.getTemporary())) {
                file.setTemporary(true);
                file.setExpirationAt(resolveExpirationAt(ctx));
            } else {
                file.setTemporary(false);
                file.setExpirationAt(null);
            }
        }
    }

    private LocalDateTime resolveExpirationAt(VfsContext ctx) {
        if (ctx.getExpirationAt() != null) {
            return ctx.getExpirationAt();
        }

        long ttlMinutes = 60L;
        if (storageProperties.getVfs() != null && storageProperties.getVfs().getTemporaryTtlMinutes() != null) {
            ttlMinutes = storageProperties.getVfs().getTemporaryTtlMinutes();
        }

        return LocalDateTime.now().plusMinutes(Math.max(ttlMinutes, 1L));
    }
}
