package com.dev.lib.storage.impl;

import com.dev.lib.config.properties.AppStorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.storage", name = "type", havingValue = "local")
public class LocalFileStorage implements StorageService {

    private final AppStorageProperties fileProperties;

    @Override
    public String upload(MultipartFile file, String path) throws IOException {
        String basePath = fileProperties.getLocal().getPath();
        File destFile = new File(basePath, path);

        if (!destFile.getParentFile().exists()) {
            destFile.getParentFile().mkdirs();
        }

        file.transferTo(destFile);
        return path;
    }

    @Override
    public byte[] download(String path) throws IOException {
        String basePath = fileProperties.getLocal().getPath();
        File file = new File(basePath, path);
        return Files.readAllBytes(file.toPath());
    }

    @Override
    public void delete(String path) {
        String basePath = fileProperties.getLocal().getPath();
        File file = new File(basePath, path);
        file.delete();
    }

    @Override
    public String getUrl(String path) {
        return fileProperties.getLocal().getUrlPrefix() + "/" + path;
    }
}