package com.dev.lib.storage.domain.service;

import com.dev.lib.storage.config.AppStorageProperties;
import io.minio.*;
import io.minio.messages.DeleteObject;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

@Component
@RequiredArgsConstructor
@ConditionalOnClass(name = "io.minio.MinioClient")
@ConditionalOnProperty(prefix = "app.storage", name = "type", havingValue = "minio")
public class MinioFileStorage implements StorageService, InitializingBean {

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

        // 确保桶存在
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(minio.getBucket()).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(minio.getBucket()).build());
        }
    }

    @Override
    public String upload(MultipartFile file, String path) throws IOException {

        String bucket = fileProperties.getMinio().getBucket();
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(path)
                            .stream(
                                    file.getInputStream(),
                                    file.getSize(),
                                    -1
                            )
                            .contentType(file.getContentType())
                            .build()
            );
            return path;
        } catch (Exception e) {
            throw new IOException(
                    "MinIO upload failed",
                    e
            );
        }
    }

    @Override
    public String upload(InputStream is, String path) throws IOException {

        String bucket = fileProperties.getMinio().getBucket();
        try {
            // 读取 InputStream 到字节数组以获取大小
            byte[] bytes = is.readAllBytes();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(path)
                            .stream(
                                    new java.io.ByteArrayInputStream(bytes),
                                    bytes.length,
                                    -1
                            )
                            .build()
            );
            return path;
        } catch (Exception e) {
            throw new IOException(
                    "MinIO upload failed",
                    e
            );
        }
    }

    @Override
    public InputStream download(String path) throws IOException {

        String bucket = fileProperties.getMinio().getBucket();
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(path)
                            .build()
            );
        } catch (Exception e) {
            throw new IOException(
                    "MinIO download failed",
                    e
            );
        }
    }

    @Override
    public void delete(String path) {

        String bucket = fileProperties.getMinio().getBucket();
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(path)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException(
                    "MinIO delete failed",
                    e
            );
        }
    }

    @Override
    public void deleteAll(Collection<String> paths) {

        if (paths == null || paths.isEmpty()) {
            return;
        }

        String bucket = fileProperties.getMinio().getBucket();
        try {
            // MinIO 原生批量删除 API
            List<DeleteObject> deleteObjects = paths.stream()
                    .map(DeleteObject::new)
                    .collect(Collectors.toList());

            minioClient.removeObjects(
                    RemoveObjectsArgs.builder()
                            .bucket(bucket)
                            .objects(deleteObjects)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException(
                    "MinIO batch delete failed",
                    e
            );
        }
    }

    @Override
    public String getPresignedUrl(String path, int expireSeconds) {

        String bucket = fileProperties.getMinio().getBucket();
        try {
            return minioClient.getPresignedObjectUrl(
                    io.minio.GetPresignedObjectUrlArgs.builder()
                            .bucket(bucket)
                            .object(path)
                            .method(io.minio.http.Method.GET)
                            .expiry(expireSeconds, java.util.concurrent.TimeUnit.SECONDS)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("MinIO getPresignedUrl failed", e);
        }
    }

    @PreDestroy
    public void destroy() {
        // MinioClient 使用 HTTP 连接池，会自动管理
        // 这里无需显式清理，但保持接口一致性
    }

    @Override
    public String copy(String sourcePath, String targetPath) throws IOException {

        String bucket = fileProperties.getMinio().getBucket();
        try {
            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .bucket(bucket)
                            .object(targetPath)
                            .source(CopySource.builder()
                                            .bucket(bucket)
                                            .object(sourcePath)
                                            .build())
                            .build()
            );
            return targetPath;
        } catch (Exception e) {
            throw new IOException("MinIO copy failed", e);
        }
    }

    @Override
    public String append(String path, String content) throws IOException {

        String bucket       = fileProperties.getMinio().getBucket();
        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);

        try {
            // MinIO 使用 ComposeObject API 实现追加
            // 1. 将新内容上传为临时对象
            String tempPath = path + ".tmp." + UUID.randomUUID();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(tempPath)
                            .stream(new ByteArrayInputStream(contentBytes), contentBytes.length, -1)
                            .build()
            );

            // 2. 检查原文件是否存在
            boolean originalExists;
            try {
                minioClient.statObject(StatObjectArgs.builder().bucket(bucket).object(path).build());
                originalExists = true;
            } catch (Exception e) {
                originalExists = false;
            }

            // 3. 使用 ComposeObject 合并
            if (originalExists) {
                // 原文件存在，合并原文件 + 临时文件
                minioClient.composeObject(
                        ComposeObjectArgs.builder()
                                .bucket(bucket)
                                .object(path)
                                .sources(asList(
                                        ComposeSource.builder().bucket(bucket).object(path).build(),
                                        ComposeSource.builder().bucket(bucket).object(tempPath).build()
                                ))
                                .build()
                );
                // 删除临时文件
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(bucket)
                                .object(tempPath)
                                .build()
                );
            } else {
                // 原文件不存在，直接上传到目标路径（覆盖临时路径）
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucket)
                                .object(path)
                                .stream(new ByteArrayInputStream(contentBytes), contentBytes.length, -1)
                                .build()
                );
                // 删除临时文件
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(bucket)
                                .object(tempPath)
                                .build()
                );
            }

            return path;
        } catch (Exception e) {
            throw new IOException("MinIO append failed", e);
        }
    }

    @Override
    public String replaceLines(String path, LineTransformer transformer) throws IOException {

        String bucket = fileProperties.getMinio().getBucket();

        // 流式处理：下载到本地临时文件 → 逐行处理 → 上传
        java.nio.file.Path tempInput  = java.nio.file.Files.createTempFile("minio-replace-in-", ".txt");
        java.nio.file.Path tempOutput = java.nio.file.Files.createTempFile("minio-replace-out-", ".txt");

        try {
            // 1. 下载原文件到本地临时文件
            try (InputStream is = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(path)
                            .build())) {
                java.nio.file.Files.copy(is, tempInput, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }

            // 2. 逐行处理（流式，不驻留内存）
            try (BufferedReader reader = java.nio.file.Files.newBufferedReader(tempInput, StandardCharsets.UTF_8);
                 BufferedWriter writer = java.nio.file.Files.newBufferedWriter(tempOutput, StandardCharsets.UTF_8)) {

                String line;
                int    lineNum = 0;
                while ((line = reader.readLine()) != null) {
                    lineNum++;
                    String transformed = transformer.transform(lineNum, line);
                    if (transformed != null) {
                        writer.write(transformed);
                        writer.newLine();
                    }
                }
            }

            // 3. 上传新文件（覆盖原文件）
            long fileSize = java.nio.file.Files.size(tempOutput);
            try (InputStream is = java.nio.file.Files.newInputStream(tempOutput)) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucket)
                                .object(path)
                                .stream(is, fileSize, -1)
                                .build()
                );
            }

            return path;
        } catch (Exception e) {
            throw new IOException("MinIO replaceLines failed", e);
        } finally {
            // 清理临时文件
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

}
