package com.dev.lib.storage.domain.service.impl;

import com.dev.lib.storage.config.AppStorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.storage", name = "type", havingValue = "local")
public class LocalFileStorage implements StorageService {

    private final AppStorageProperties fileProperties;

    @Override
    public String upload(MultipartFile file, String path) throws IOException {

        File destFile = resolveSafePath(path);

        if (!destFile.getParentFile().exists()) {
            destFile.getParentFile().mkdirs();
        }

        file.transferTo(destFile);
        return path;
    }

    @Override
    public InputStream download(String path) throws IOException {

        File file = resolveSafePath(path);
        return new FileInputStream(file);
    }

    @Override
    public void delete(String path) {

        String basePath = fileProperties.getLocal().getPath();
        File   file     = new File(
                basePath,
                path
        );
        file.delete();
    }

    @Override
    public String getUrl(String path) {

        return fileProperties.getLocal().getUrlPrefix() + "/" + path;
    }

    private File resolveSafePath(String path) throws IOException {

        String basePath = fileProperties.getLocal().getPath();
        File   base     = new File(basePath).getCanonicalFile();
        File   target   = new File(
                base,
                path
        ).getCanonicalFile();

        if (!target.getPath().startsWith(base.getPath() + File.separator)) {
            throw new IOException("Invalid path: " + path);
        }

        return target;
    }

}