package com.dev.lib.storage;

import com.dev.lib.config.properties.AppStorageProperties;
import com.dev.lib.entity.id.IDWorker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class FileService {

    private final StorageService storage;
    private final AppStorageProperties fileProperties;
    private final SysFileRepository fileRepository;

    public SysFile upload(MultipartFile file, String category) throws IOException {
        // 校验文件
        validateFile(file);

        // 生成存储路径
        String extension = getExtension(file.getOriginalFilename());
        String storageName = generateFileName() + "." + extension;
        String storagePath = generatePath(category, storageName);

        // 计算MD5(去重)
        String md5 = calculateMd5(file);
        SysFile existFile = fileRepository.findByMd5(md5);
        if (existFile != null) {
            return existFile;  // 秒传
        }

        // 上传文件
        storage.upload(file, storagePath);

        // 保存记录
        SysFile sysFile = new SysFile();
        sysFile.setOriginalName(file.getOriginalFilename());
        sysFile.setStorageName(storageName);
        sysFile.setStoragePath(storagePath);
        sysFile.setUrl(storage.getUrl(storagePath));
        sysFile.setExtension(extension);
        sysFile.setContentType(file.getContentType());
        sysFile.setSize(file.getSize());
        sysFile.setStorageType(fileProperties.getType());
        sysFile.setMd5(md5);
        sysFile.setCategory(category);

        return fileRepository.save(sysFile);
    }

    public byte[] download(Long fileId) throws IOException {
        SysFile file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));

        return storage.download(file.getStoragePath());
    }

    public void delete(Long fileId) {
        SysFile file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));

        storage.delete(file.getStoragePath());
        fileRepository.delete(file);
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        if (file.getSize() > fileProperties.getMaxSize()) {
            throw new IllegalArgumentException("File size exceeds limit");
        }

        String extension = getExtension(file.getOriginalFilename());
        String[] allowed = fileProperties.getAllowedExtensions().split(",");
        if (!Arrays.asList(allowed).contains(extension)) {
            throw new IllegalArgumentException("File type not allowed");
        }
    }

    private String getExtension(String filename) {
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    private String generateFileName() {
        return IDWorker.newId();
    }

    private String generatePath(String category, String filename) {
        LocalDate now = LocalDate.now();
        return String.format(
                "%s/%d/%02d/%02d/%s",
                category, now.getYear(), now.getMonthValue(), now.getDayOfMonth(), filename
        );
    }

    private String calculateMd5(MultipartFile file) throws IOException {
        return DigestUtils.md5DigestAsHex(file.getInputStream());
    }
}