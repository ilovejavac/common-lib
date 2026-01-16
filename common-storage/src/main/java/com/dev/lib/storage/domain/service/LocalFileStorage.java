package com.dev.lib.storage.domain.service;

import com.dev.lib.storage.config.AppStorageProperties;
import com.dev.lib.util.parallel.ParallelExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collection;

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

    @Override
    public String copy(String sourcePath, String targetPath) throws IOException {

        File sourceFile = resolveSafePath(sourcePath);
        File targetFile = resolveSafePath(targetPath);

        if (!targetFile.getParentFile().exists()) {
            targetFile.getParentFile().mkdirs();
        }

        Files.copy(
                sourceFile.toPath(),
                targetFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING
        );
        return targetPath;
    }

    @Override
    public String append(String path, String content) throws IOException {

        File file = resolveSafePath(path);

        try (FileWriter fw = new FileWriter(file, true)) {
            fw.write(content);
        }
        return path;
    }

    @Override
    public String replaceLines(String path, LineTransformer transformer) throws IOException {

        File sourceFile = resolveSafePath(path);
        File tempFile   = new File(sourceFile.getParentFile(), sourceFile.getName() + ".tmp." + System.nanoTime());

        try (BufferedReader reader = new BufferedReader(new FileReader(sourceFile, StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile, StandardCharsets.UTF_8))) {

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

        // 原子替换
        Files.move(
                tempFile.toPath(),
                sourceFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
        );
        return path;
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
