package com.dev.lib.storage.domain.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

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
    public String getUrl(String path) {

        return "";
    }

    @Override
    public void afterPropertiesSet() throws Exception {

    }

}
