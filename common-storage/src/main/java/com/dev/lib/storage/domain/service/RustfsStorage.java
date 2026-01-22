package com.dev.lib.storage.domain.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

@Component
@RequiredArgsConstructor
//@ConditionalOnClass(name = "com.aliyun.oss.OSS")
@ConditionalOnProperty(prefix = "app.storage", name = "type", havingValue = "rustfs")
public class RustfsStorage implements StorageService, InitializingBean {

    @Override
    public String upload(MultipartFile file, String path) throws IOException {

        return "";
    }

    @Override
    public InputStream download(String path) throws IOException {

        return null;
    }

    @Override
    public void delete(String path) {

    }

    @Override
    public void deleteAll(Collection<String> paths) {

        if (paths == null || paths.isEmpty()) {
            return;
        }
        // RustFS 批量删除实现
    }

    @Override
    public void afterPropertiesSet() throws Exception {

    }

    @Override
    public String copy(String sourcePath, String targetPath) throws IOException {

        return "";
    }

    @Override
    public String append(String path, String content) throws IOException {

        return "";
    }

    @Override
    public String replaceLines(String path, LineTransformer transformer) throws IOException {
        // RustFS 待实现
        throw new UnsupportedOperationException("RustfsStorage.replaceLines not implemented yet");
    }

}
