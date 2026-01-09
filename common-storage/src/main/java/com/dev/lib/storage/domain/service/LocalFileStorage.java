package com.dev.lib.storage.domain.service;

import com.dev.lib.storage.config.AppStorageProperties;
import com.dev.lib.util.parallel.ParallelExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.storage", name = "type", havingValue = "local")
public class LocalFileStorage implements StorageService {

    private final AppStorageProperties fileProperties;

    @Override
    public String upload(MultipartFile file, String path) throws IOException {

        return upload(file.getInputStream(), path);
    }

    @Override
    public String upload(InputStream is, String path) throws IOException {

        File destFile = resolveSafePath(path);

        if (!destFile.getParentFile().exists()) {
            destFile.getParentFile().mkdirs();
        }

        try (FileOutputStream fos = new FileOutputStream(destFile)) {
            is.transferTo(fos);
        }
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
        File   file     = new File(basePath, path);
        file.delete();
    }

    @Override
    public void deleteAll(Collection<String> paths) {

        if (paths == null || paths.isEmpty()) {
            return;
        }

        String basePath = fileProperties.getLocal().getPath();

        // 批量删除：一次性收集所有文件并删除
        ParallelExecutor.with(paths).apply(path -> {
            File file = new File(basePath, path);
            if (file.exists()) {
                file.delete();
            }
        });
    }

    @Override
    public String getUrl(String path) {

        return fileProperties.getLocal().getUrlPrefix() + "/" + path;
    }

    private File resolveSafePath(String path) throws IOException {

        String basePath = fileProperties.getLocal().getPath();
        File   base     = new File(basePath).getCanonicalFile();
        File target = new File(
                base,
                path
        ).getCanonicalFile();

        if (!target.getPath().startsWith(base.getPath() + File.separator)) {
            throw new IOException("Invalid path: " + path);
        }

        return target;
    }

}
