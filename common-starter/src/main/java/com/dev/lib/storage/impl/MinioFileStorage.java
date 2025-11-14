package com.dev.lib.storage.impl;

import com.dev.lib.config.properties.AppStorageProperties;
import com.dev.lib.storage.StorageService;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.storage", name = "type", havingValue = "minio")
public class MinioFileStorage implements StorageService, InitializingBean {

    private final AppStorageProperties fileProperties;
    private MinioClient minioClient;

    @Override
    public void afterPropertiesSet() throws Exception {
        AppStorageProperties.Minio minio = fileProperties.getMinio();
        minioClient = MinioClient.builder()
                .endpoint(minio.getEndpoint())
                .credentials(minio.getAccessKey(), minio.getSecretKey())
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
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            return path;
        } catch (Exception e) {
            throw new IOException("MinIO upload failed", e);
        }
    }

    @Override
    public byte[] download(String path) throws IOException {
        String bucket = fileProperties.getMinio().getBucket();
        try (InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucket)
                        .object(path)
                        .build()
        )) {
            return stream.readAllBytes();
        } catch (Exception e) {
            throw new IOException("MinIO download failed", e);
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
            throw new RuntimeException("MinIO delete failed", e);
        }
    }

    @Override
    public String getUrl(String path) {
        String endpoint = fileProperties.getMinio().getEndpoint();
        String bucket = fileProperties.getMinio().getBucket();
        return String.format("%s/%s/%s", endpoint, bucket, path);
    }
}