package com.dev.lib.storage;

import com.dev.lib.config.properties.AppStorageProperties;
import com.dev.lib.entity.id.IDWorker;
import com.dev.lib.jpa.data.SysFile;
import com.dev.lib.jpa.data.SysFileRepository;
import com.dev.lib.jpa.data.SysFileToFileItemMapper;
import com.dev.lib.storage.impl.StorageService;
import com.dev.lib.storage.serialize.FileItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    public static final String FILE_NOT_FOUND = "File not found";
    private final StorageService storage;
    private final AppStorageProperties fileProperties;

    private final SysFileRepository fileRepository;

    public SysFile upload(MultipartFile file, String category) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("File not exists");
        }
        // 校验文件
        validateFile(file);

        // 生成存储路径
        String extension = getExtension(file.getOriginalFilename());
        String storageName = generateFileName() + "." + extension;
        String storagePath = generatePath(category, storageName);

        // 计算MD5(去重)
        String md5 = calculateMd5(file);
        Optional<SysFile> existFile = fileRepository.findByMd5(md5);
        if (existFile.isPresent()) {
            return existFile.get();
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

    SysFile getById(String id) {
        return fileRepository.findByBizId(id).orElseThrow(() -> new RuntimeException(FILE_NOT_FOUND));
    }

    public byte[] download(SysFile sf) throws IOException {
        SysFile file = fileRepository.findById(sf.getId())
                .orElseThrow(() -> new RuntimeException(FILE_NOT_FOUND));

        return storage.download(file.getStoragePath());
    }

    public void delete(SysFile sf) {
        SysFile file = fileRepository.findById(sf.getId())
                .orElseThrow(() -> new RuntimeException(FILE_NOT_FOUND));

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
        if (!StringUtils.hasText(filename)) {
            throw new IllegalArgumentException("unknow filename");
        }
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

    private final SysFileToFileItemMapper mapper;

    public FileItem getItem(String value) {
        return mapper.convert(fileRepository.findByBizId(value).orElse(null));
    }
}