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

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

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
    public String getUrl(String path) {

        String endpoint = fileProperties.getMinio().getEndpoint();
        String bucket   = fileProperties.getMinio().getBucket();
        return String.format(
                "%s/%s/%s",
                endpoint,
                bucket,
                path
        );
    }

    @PreDestroy
    public void destroy() {
        // MinioClient 使用 HTTP 连接池，会自动管理
        // 这里无需显式清理，但保持接口一致性
    }

}
