package com.dev.lib.storage.domain.service;

import com.dev.lib.storage.config.AppStorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

/**
 * 本地文件系统链式存储服务实现
 *
 * <p>支持动态 bucket（作为根路径前缀），用于链式 API 调用</p>
 */
@Slf4j
@Component
@Primary
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.storage", name = "type", havingValue = "local", matchIfMissing = false)
public class LocalChainStorage implements ChainStorageService, InitializingBean {

    private final AppStorageProperties fileProperties;

    private String basePath;

    @Override
    public void afterPropertiesSet() throws Exception {
        this.basePath = fileProperties.getLocal().getPath();
        // 确保根目录存在
        Files.createDirectories(Path.of(basePath));
    }

    private Path resolvePath(String bucketName, String objectKey) {
        // bucketName 作为子目录
        Path bucketPath = Path.of(basePath, bucketName);
        return bucketPath.resolve(objectKey);
    }

    @Override
    public String upload(String bucketName, String objectKey, MultipartFile file) throws IOException {
        return upload(bucketName, objectKey, file.getInputStream());
    }

    @Override
    public String upload(String bucketName, String objectKey, InputStream inputStream) throws IOException {
        Path targetPath = resolvePath(bucketName, objectKey);
        // 确保父目录存在
        Files.createDirectories(targetPath.getParent());
        Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        return objectKey;
    }

    @Override
    public InputStream download(String bucketName, String objectKey) throws IOException {
        Path filePath = resolvePath(bucketName, objectKey);
        return Files.newInputStream(filePath);
    }

    @Override
    public void delete(String bucketName, String objectKey) {
        try {
            Path filePath = resolvePath(bucketName, objectKey);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Local delete failed", e);
        }
    }

    @Override
    public String getPresignedUrl(String bucketName, String objectKey, int expireSeconds) {
        // 本地存储返回文件路径或 URL 前缀
        String urlPrefix = fileProperties.getLocal().getUrlPrefix();
        if (urlPrefix != null && !urlPrefix.isBlank()) {
            return urlPrefix + "/" + bucketName + "/" + objectKey;
        }
        return "file://" + resolvePath(bucketName, objectKey).toAbsolutePath();
    }

    @Override
    public String copy(String bucketName, String sourceKey, String targetKey) throws IOException {
        Path sourcePath = resolvePath(bucketName, sourceKey);
        Path targetPath = resolvePath(bucketName, targetKey);
        Files.createDirectories(targetPath.getParent());
        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        return targetKey;
    }

    @Override
    public String append(String bucketName, String objectKey, String content) throws IOException {
        Path filePath = resolvePath(bucketName, objectKey);
        Files.createDirectories(filePath.getParent());
        Files.writeString(filePath, content, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        return objectKey;
    }

    @Override
    public String appendBytes(String bucketName, String objectKey, byte[] bytes) throws IOException {
        Path filePath = resolvePath(bucketName, objectKey);
        Files.createDirectories(filePath.getParent());
        Files.write(filePath, bytes, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        return objectKey;
    }

    @Override
    public String write(String bucketName, String objectKey, String content) throws IOException {
        Path filePath = resolvePath(bucketName, objectKey);
        Files.createDirectories(filePath.getParent());
        Files.writeString(filePath, content, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        return objectKey;
    }

    @Override
    public String writeBytes(String bucketName, String objectKey, byte[] bytes) throws IOException {
        Path filePath = resolvePath(bucketName, objectKey);
        Files.createDirectories(filePath.getParent());
        Files.write(filePath, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        return objectKey;
    }

    @Override
    public String replaceLines(String bucketName, String objectKey, StorageService.LineTransformer transformer) throws IOException {
        Path filePath = resolvePath(bucketName, objectKey);
        Path tempPath = Files.createTempFile("local-replace-", ".txt");

        try {
            // 1. 逐行处理
            try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8);
                 BufferedWriter writer = Files.newBufferedWriter(tempPath, StandardCharsets.UTF_8)) {

                String line;
                int lineNum = 0;
                while ((line = reader.readLine()) != null) {
                    lineNum++;
                    String transformed = transformer.transform(lineNum, line);
                    if (transformed != null) {
                        writer.write(transformed);
                        writer.newLine();
                    }
                }
            }

            // 2. 替换原文件
            Files.createDirectories(filePath.getParent());
            Files.move(tempPath, filePath, StandardCopyOption.REPLACE_EXISTING);

            return objectKey;
        } catch (Exception e) {
            throw new IOException("Local replaceLines failed", e);
        } finally {
            try {
                Files.deleteIfExists(tempPath);
            } catch (Exception ignored) {
            }
        }
    }
}
