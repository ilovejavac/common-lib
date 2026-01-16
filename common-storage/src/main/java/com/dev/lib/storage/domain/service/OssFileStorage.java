package com.dev.lib.storage.domain.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.AppendObjectRequest;
import com.aliyun.oss.model.DeleteObjectsRequest;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.ObjectMetadata;
import com.dev.lib.storage.config.AppStorageProperties;
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

@Component
@RequiredArgsConstructor
@ConditionalOnClass(name = "com.aliyun.oss.OSS")
@ConditionalOnProperty(prefix = "app.storage", name = "type", havingValue = "oss")
public class OssFileStorage implements StorageService, InitializingBean {

    private final AppStorageProperties fileProperties;

    private OSS ossClient;

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
    public String upload(MultipartFile file, String path) throws IOException {

        return upload(file.getInputStream(), path);
    }

    @Override
    public String upload(InputStream is, String path) throws IOException {

        String bucket = fileProperties.getOss().getBucket();
        ossClient.putObject(
                bucket,
                path,
                is
        );
        return path;
    }

    @Override
    public InputStream download(String path) throws IOException {

        String bucket = fileProperties.getOss().getBucket();
        OSSObject ossObject = ossClient.getObject(
                bucket,
                path
        );
        return ossObject.getObjectContent();
    }

    @Override
    public void delete(String path) {

        String bucket = fileProperties.getOss().getBucket();
        ossClient.deleteObject(
                bucket,
                path
        );
    }

    @Override
    public void deleteAll(Collection<String> paths) {

        if (paths == null || paths.isEmpty()) {
            return;
        }

        String bucket = fileProperties.getOss().getBucket();

        // 阿里云 OSS 原生批量删除 API
        List<String>         pathList      = List.copyOf(paths);
        DeleteObjectsRequest deleteRequest = new DeleteObjectsRequest(bucket);
        deleteRequest.setKeys(pathList);

        ossClient.deleteObjects(deleteRequest);
    }

    @Override
    public String getUrl(String path) {

        String bucket   = fileProperties.getOss().getBucket();
        String endpoint = fileProperties.getOss().getEndpoint();
        return String.format(
                "https://%s.%s/%s",
                bucket,
                endpoint,
                path
        );
    }

    @PreDestroy
    public void destroy() {

        if (ossClient != null) {
            ossClient.shutdown();
        }
    }

    @Override
    public String copy(String sourcePath, String targetPath) throws IOException {

        String bucket = fileProperties.getOss().getBucket();
        try {
            ossClient.copyObject(
                    bucket,
                    sourcePath,
                    bucket,
                    targetPath
            );
            return targetPath;
        } catch (Exception e) {
            throw new IOException("OSS copy failed", e);
        }
    }

    @Override
    public String append(String path, String content) throws IOException {

        String bucket       = fileProperties.getOss().getBucket();
        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);

        try {
            // 获取文件当前位置（追加需要 position）
            long position;
            try {
                position = ossClient.getObjectMetadata(bucket, path).getContentLength();
            } catch (Exception e) {
                // 文件不存在，position 从 0 开始
                position = 0;
            }

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(contentBytes.length);

            AppendObjectRequest appendRequest = new AppendObjectRequest(
                    bucket, path,
                    new ByteArrayInputStream(contentBytes), metadata
            );
            appendRequest.setPosition(position);
            ossClient.appendObject(appendRequest);
            return path;
        } catch (Exception e) {
            throw new IOException("OSS append failed", e);
        }
    }

    @Override
    public String replaceLines(String path, LineTransformer transformer) throws IOException {

        String bucket = fileProperties.getOss().getBucket();

        // 流式处理：下载到本地临时文件 → 逐行处理 → 上传
        java.nio.file.Path tempInput  = java.nio.file.Files.createTempFile("oss-replace-in-", ".txt");
        java.nio.file.Path tempOutput = java.nio.file.Files.createTempFile("oss-replace-out-", ".txt");

        try {
            // 1. 下载原文件到本地临时文件
            try (InputStream is = ossClient.getObject(bucket, path).getObjectContent()) {
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
                ossClient.putObject(bucket, path, is);
            }

            return path;
        } catch (Exception e) {
            throw new IOException("OSS replaceLines failed", e);
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
