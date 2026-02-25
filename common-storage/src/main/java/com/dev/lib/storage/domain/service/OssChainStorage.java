package com.dev.lib.storage.domain.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.AppendObjectRequest;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.ObjectMetadata;
import com.dev.lib.storage.config.AppStorageProperties;
import com.dev.lib.storage.data.FileSystemRepository;
import com.dev.lib.storage.domain.service.chain.AbstractChainStorage;
import com.dev.lib.storage.domain.service.chain.ChainStorageService;
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
import java.io.FilterInputStream;

/**
 * 阿里云 OSS 链式存储服务实现
 *
 * <p>支持动态 bucket，用于链式 API 调用</p>
 */
@Slf4j
@Component
@Primary
@ConditionalOnClass(name = "com.aliyun.oss.OSS")
@ConditionalOnProperty(prefix = "app.storage", name = "type", havingValue = "oss")
public class OssChainStorage extends AbstractChainStorage implements ChainStorageService, InitializingBean {

    private OSS ossClient;

    public OssChainStorage(AppStorageProperties fileProperties, FileSystemRepository fileRepository) {
        super(fileProperties, fileRepository);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        AppStorageProperties.Oss oss = fileProperties.getOss();
        ossClient = new OSSClientBuilder().build(
                oss.getEndpoint(),
                oss.getAccessKey(),
                oss.getSecretKey()
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String upload(String bucketName, String objectKey, MultipartFile file) throws IOException {
        return upload(bucketName, objectKey, file.getInputStream());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String upload(String bucketName, String objectKey, InputStream inputStream) throws IOException {
        ensureBucketExists(bucketName);
        // 读取 InputStream 到字节数组以获取大小（参考历史 commit 12536ee）
        byte[] bytes = inputStream.readAllBytes();
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(bytes.length);
        ossClient.putObject(bucketName, objectKey, new ByteArrayInputStream(bytes), metadata);

        // 同步数据库记录
        saveFileRecord(bucketName, objectKey, (long) bytes.length);

        return objectKey;
    }

    @Override
    public InputStream download(String bucketName, String objectKey) throws IOException {
        OSSObject ossObject = ossClient.getObject(bucketName, objectKey);
        // 包装 InputStream 以在关闭时同时关闭 OSSObject，避免资源泄漏
        return new FilterInputStream(ossObject.getObjectContent()) {
            private boolean closed = false;

            @Override
            public void close() throws IOException {
                if (!closed) {
                    closed = true;
                    super.close();
                    // 显式关闭 OSSObject 以释放 HTTP 连接
                    ossObject.close();
                }
            }
        };
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String bucketName, String objectKey) {
        ossClient.deleteObject(bucketName, objectKey);

        // 同步删除数据库记录
        deleteFileRecord(bucketName, objectKey);
    }

    @Override
    public String getPresignedUrl(String bucketName, String objectKey, int expireSeconds) {
        java.util.Date expiration = new java.util.Date(
                System.currentTimeMillis() + expireSeconds * 1000L
        );
        return ossClient.generatePresignedUrl(bucketName, objectKey, expiration).toString();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String copy(String bucketName, String sourceKey, String targetKey) throws IOException {
        try {
            ossClient.copyObject(bucketName, sourceKey, bucketName, targetKey);

            // 获取源文件大小并同步数据库记录
            ObjectMetadata metadata = ossClient.getObjectMetadata(bucketName, sourceKey);
            saveFileRecord(bucketName, targetKey, metadata.getContentLength());

            return targetKey;
        } catch (Exception e) {
            throw new IOException("OSS copy failed", e);
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
            // 获取文件当前位置（追加需要 position）
            long position;
            try {
                position = ossClient.getObjectMetadata(bucketName, objectKey).getContentLength();
            } catch (Exception e) {
                position = 0;
            }

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(bytes.length);

            AppendObjectRequest appendRequest = new AppendObjectRequest(
                    bucketName, objectKey,
                    new ByteArrayInputStream(bytes), metadata
            );
            appendRequest.setPosition(position);
            ossClient.appendObject(appendRequest);

            // 更新数据库记录
            saveFileRecord(bucketName, objectKey, position + bytes.length);

            return objectKey;
        } catch (Exception e) {
            throw new IOException("OSS append failed", e);
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
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(bytes.length);
        ossClient.putObject(bucketName, objectKey, new ByteArrayInputStream(bytes), metadata);

        // 同步数据库记录
        saveFileRecord(bucketName, objectKey, (long) bytes.length);

        return objectKey;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String replaceLines(String bucketName, String objectKey, StorageService.LineTransformer transformer) throws IOException {
        java.nio.file.Path tempInput = java.nio.file.Files.createTempFile("oss-replace-in-", ".txt");
        java.nio.file.Path tempOutput = java.nio.file.Files.createTempFile("oss-replace-out-", ".txt");

        try {
            // 1. 下载原文件到本地临时文件
            try (InputStream is = ossClient.getObject(bucketName, objectKey).getObjectContent()) {
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
                ossClient.putObject(bucketName, objectKey, is);
            }

            // 更新数据库记录
            saveFileRecord(bucketName, objectKey, fileSize);

            return objectKey;
        } catch (Exception e) {
            throw new IOException("OSS replaceLines failed", e);
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

    private void ensureBucketExists(String bucketName) {
        if (!ossClient.doesBucketExist(bucketName)) {
            ossClient.createBucket(bucketName);
            log.info("Created bucket: {}", bucketName);
        }
    }

    @PreDestroy
    public void destroy() {
        if (ossClient != null) {
            ossClient.shutdown();
        }
    }
}
