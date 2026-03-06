package com.dev.lib.storage.domain.service.chain;

import com.dev.lib.storage.config.AppStorageProperties;
import com.dev.lib.storage.data.VfsPathRepository;
import com.dev.lib.storage.Storage;
import com.dev.lib.storage.domain.service.virtual.StorageServiceNameProvider;
import com.dev.lib.storage.domain.service.write.SysFileCowService;
import io.minio.*;
import io.minio.http.Method;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;

import static java.util.Arrays.asList;

/**
 * MinIO 链式存储服务实现
 *
 * <p>支持动态 bucket，用于链式 API 调用</p>
 */
@Slf4j
@Component
@Primary
@ConditionalOnClass(name = "io.minio.MinioClient")
@ConditionalOnProperty(prefix = "app.storage", name = "type", havingValue = "minio")
public class MinioChainStorage extends AbstractChainStorage implements ChainStorageService, InitializingBean {

    private MinioClient minioClient;

    public MinioChainStorage(
            AppStorageProperties fileProperties,
            VfsPathRepository fileRepository,
            StorageServiceNameProvider serviceNameProvider,
            SysFileCowService sysFileCowService
    ) {
        super(fileProperties, fileRepository, serviceNameProvider, sysFileCowService);
    }

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
    @Transactional(rollbackFor = Exception.class)
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

            // 同步数据库记录并返回 bizId
            return saveFileRecord(bucketName, objectKey, file.getSize());
        } catch (Exception e) {
            throw new IOException("MinIO upload failed", e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String upload(String bucketName, String objectKey, InputStream inputStream) throws IOException {
        ensureBucketExists(bucketName);
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .stream(inputStream, -1, -1)  // 流式上传，大小未知
                            .build()
            );

            // 注意：由于未知文件大小，无法同步数据库记录的 size 字段
            // 将保存记录但 size 为 null，并返回 bizId
            return saveFileRecord(bucketName, objectKey, null);
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
    @Transactional(rollbackFor = Exception.class)
    public void delete(String bucketName, String objectKey) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .build()
            );

            // 同步删除数据库记录
            deleteFileRecord(bucketName, objectKey);
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
    @Transactional(rollbackFor = Exception.class)
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

            // 获取源文件大小并同步数据库记录，返回 bizId
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder().bucket(bucketName).object(sourceKey).build()
            );
            return saveFileRecord(bucketName, targetKey, stat.size());
        } catch (Exception e) {
            throw new IOException("MinIO copy failed", e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String append(String bucketName, String objectKey, String content) throws IOException {
        return appendBytes(bucketName, objectKey, content.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String appendBytes(String bucketName, String objectKey, byte[] bytes) throws IOException {
        ensureBucketExists(bucketName);

        try {
            // 1. 将新内容上传为临时对象
            String tempPath = objectKey + ".tmp." + com.dev.lib.entity.id.IDWorker.newId();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(tempPath)
                            .stream(new ByteArrayInputStream(bytes), bytes.length, -1)
                            .build()
            );

            // 2. 检查原文件是否存在
            boolean originalExists;
            long originalSize = 0;
            try {
                StatObjectResponse stat = minioClient.statObject(
                        StatObjectArgs.builder().bucket(bucketName).object(objectKey).build()
                );
                originalExists = true;
                originalSize = stat.size();
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

                // 更新数据库记录并返回 bizId
                return saveFileRecord(bucketName, objectKey, originalSize + bytes.length);
            } else {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucketName)
                                .object(objectKey)
                                .stream(new ByteArrayInputStream(bytes), bytes.length, -1)
                                .build()
                );
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(bucketName)
                                .object(tempPath)
                                .build()
                );

                // 创建数据库记录并返回 bizId
                return saveFileRecord(bucketName, objectKey, (long) bytes.length);
            }
        } catch (Exception e) {
            throw new IOException("MinIO append failed", e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String write(String bucketName, String objectKey, String content) throws IOException {
        return writeBytes(bucketName, objectKey, content.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String writeBytes(String bucketName, String objectKey, byte[] bytes) throws IOException {
        ensureBucketExists(bucketName);
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .stream(new ByteArrayInputStream(bytes), bytes.length, -1)
                            .build()
            );

            // 同步数据库记录并返回 bizId
            return saveFileRecord(bucketName, objectKey, (long) bytes.length);
        } catch (Exception e) {
            throw new IOException("MinIO write failed", e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String replaceLines(String bucketName, String objectKey, Storage.LineTransformer transformer) throws IOException {
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

            // 更新数据库记录并返回 bizId
            return saveFileRecord(bucketName, objectKey, fileSize);
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
