package com.dev.lib.storage.domain.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.DeleteObjectsRequest;
import com.aliyun.oss.model.OSSObject;
import com.dev.lib.storage.config.AppStorageProperties;
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

        String bucket = fileProperties.getOss().getBucket();
        ossClient.putObject(
                bucket,
                path,
                file.getInputStream()
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
        List<String> pathList = List.copyOf(paths);
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

}
