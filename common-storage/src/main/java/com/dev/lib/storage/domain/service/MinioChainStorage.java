package com.dev.lib.storage.domain.service;

import com.dev.lib.storage.config.AppStorageProperties;
import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.DeleteObject;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

/**
 * MinIO 链式存储服务实现
 *
 * <p>支持动态 bucket，用于链式 API 调用</p>
 */
@Slf4j
@Component
@Primary
@RequiredArgsConstructor
@ConditionalOnClass(name = "io.minio.MinioClient")
@ConditionalOnProperty(prefix = "app.storage", name = "type", havingValue = "minio")
public class MinioChainStorage implements ChainStorageService, InitializingBean {

    private final AppStorageProperties fileProperties;

    private MinioClient minioClient;

    @Override
    public void afterPropertiesSet() throws Exception {
        AppStorageProperties.Minio minio = fileProperties.getMinio();
        minioClient = MinioClient.builder()
                .endpoint(minio.getEndpoint())
                .credentials(
                        minio.getAccessKey(),
                        minio.getSecretKey()
                )
                .build();
    }

    private void ensureBucketExists(String bucketName) {
        try {
            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("Created bucket: {}", bucketName);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to ensure bucket exists: " + bucketName, e);
        }
    }

    @Override
    public String upload(String bucketName, String objectKey, MultipartFile file) throws IOException {
        ensureBucketExists(bucketName);
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .build()
            );
            return objectKey;
        } catch (Exception e) {
            throw new IOException("MinIO upload failed", e);
        }
    }

    @Override
    public String upload(String bucketName, String objectKey, InputStream inputStream) throws IOException {
        ensureBucketExists(bucketName);
        try {
            byte[] bytes = inputStream.readAllBytes();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .stream(new ByteArrayInputStream(bytes), bytes.length, -1)
                            .build()
            );
            return objectKey;
        } catch (Exception e) {
            throw new IOException("MinIO upload failed", e);
        }
    }

    @Override
    public InputStream download(String bucketName, String objectKey) throws IOException {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .build()
            );
        } catch (Exception e) {
            throw new IOException("MinIO download failed", e);
        }
    }

    @Override
    public void delete(String bucketName, String objectKey) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("MinIO delete failed", e);
        }
    }

    @Override
    public String getPresignedUrl(String bucketName, String objectKey, int expireSeconds) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .method(Method.GET)
                            .expiry(expireSeconds, java.util.concurrent.TimeUnit.SECONDS)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("MinIO getPresignedUrl failed", e);
        }
    }

    @Override
    public String copy(String bucketName, String sourceKey, String targetKey) throws IOException {
        try {
            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .bucket(bucketName)
                            .object(targetKey)
                            .source(CopySource.builder()
                                    .bucket(bucketName)
                                    .object(sourceKey)
                                    .build())
                            .build()
            );
            return targetKey;
        } catch (Exception e) {
            throw new IOException("MinIO copy failed", e);
        }
    }

    @Override
    public String append(String bucketName, String objectKey, String content) throws IOException {
        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);

        try {
            // 1. 将新内容上传为临时对象
            String tempPath = objectKey + ".tmp." + java.util.UUID.randomUUID();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(tempPath)
                            .stream(new ByteArrayInputStream(contentBytes), contentBytes.length, -1)
                            .build()
            );

            // 2. 检查原文件是否存在
            boolean originalExists;
            try {
                minioClient.statObject(StatObjectArgs.builder().bucket(bucketName).object(objectKey).build());
                originalExists = true;
            } catch (Exception e) {
                originalExists = false;
            }

            // 3. 使用 ComposeObject 合并
            if (originalExists) {
                minioClient.composeObject(
                        ComposeObjectArgs.builder()
                                .bucket(bucketName)
                                .object(objectKey)
                                .sources(asList(
                                        ComposeSource.builder().bucket(bucketName).object(objectKey).build(),
                                        ComposeSource.builder().bucket(bucketName).object(tempPath).build()
                                ))
                                .build()
                );
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(bucketName)
                                .object(tempPath)
                                .build()
                );
            } else {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucketName)
                                .object(objectKey)
                                .stream(new ByteArrayInputStream(contentBytes), contentBytes.length, -1)
                                .build()
                );
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(bucketName)
                                .object(tempPath)
                                .build()
                );
            }

            return objectKey;
        } catch (Exception e) {
            throw new IOException("MinIO append failed", e);
        }
    }

    @Override
    public String replaceLines(String bucketName, String objectKey, StorageService.LineTransformer transformer) throws IOException {
        java.nio.file.Path tempInput = java.nio.file.Files.createTempFile("minio-replace-in-", ".txt");
        java.nio.file.Path tempOutput = java.nio.file.Files.createTempFile("minio-replace-out-", ".txt");

        try {
            // 1. 下载原文件到本地临时文件
            try (InputStream is = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .build())) {
                java.nio.file.Files.copy(is, tempInput, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }

            // 2. 逐行处理
            try (BufferedReader reader = java.nio.file.Files.newBufferedReader(tempInput, StandardCharsets.UTF_8);
                 BufferedWriter writer = java.nio.file.Files.newBufferedWriter(tempOutput, StandardCharsets.UTF_8)) {

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

            // 3. 上传新文件
            long fileSize = java.nio.file.Files.size(tempOutput);
            try (InputStream is = java.nio.file.Files.newInputStream(tempOutput)) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucketName)
                                .object(objectKey)
                                .stream(is, fileSize, -1)
                                .build()
                );
            }

            return objectKey;
        } catch (Exception e) {
            throw new IOException("MinIO replaceLines failed", e);
        } finally {
            try {
                java.nio.file.Files.deleteIfExists(tempInput);
            } catch (Exception ignored) {
            }
            try {
                java.nio.file.Files.deleteIfExists(tempOutput);
            } catch (Exception ignored) {
            }
        }
    }

    @PreDestroy
    public void destroy() {
        // MinioClient 使用 HTTP 连接池，会自动管理
    }
}
