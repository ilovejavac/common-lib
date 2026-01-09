package com.dev.lib.storage.domain.service;

import com.dev.lib.entity.id.IDWorker;
import com.dev.lib.storage.config.AppStorageProperties;
import com.dev.lib.storage.domain.adapter.StorageFileRepo;
import com.dev.lib.storage.domain.model.StorageFile;
import com.dev.lib.storage.domain.model.StorageFileToFileItemMapper;
import com.dev.lib.storage.serialize.FileItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final AppStorageProperties fileProperties;

    private final StorageService storage;

    private final StorageFileRepo repo;

    @Override
    public StorageFile upload(InputStream is, String category) throws IOException {

        String storageName = generateFileName();
        String storagePath = generatePath(category, storageName);

        storage.upload(is, storagePath);

        StorageFile sf = new StorageFile();
        sf.setStorageName(storageName);
        sf.setStoragePath(storagePath);
        sf.setUrl(storage.getUrl(storagePath));
        sf.setStorageType(fileProperties.getType());
        sf.setCategory(category);

        repo.saveFile(sf);
        return sf;
    }

    public StorageFile upload(MultipartFile file, String category) throws IOException {

        if (file == null) {
            throw new IllegalArgumentException("File not exists");
        }
        // 校验文件
        validateFile(file);

        // 生成存储路径
        String extension   = getExtension(file.getOriginalFilename());
        String storageName = generateFileName() + "." + extension;
        String storagePath = generatePath(
                category,
                storageName
        );

        // 上传文件
        storage.upload(
                file,
                storagePath
        );
        StorageFile sf = new StorageFile();
        sf.setOriginalName(file.getOriginalFilename());
        sf.setStorageName(storageName);
        sf.setStoragePath(storagePath);
        sf.setUrl(storage.getUrl(storagePath));
        sf.setExtension(extension);
        sf.setContentType(file.getContentType());
        sf.setSize(file.getSize());
        sf.setStorageType(fileProperties.getType());
        sf.setCategory(category);

        repo.saveFile(sf);
        return sf;
    }

    public StorageFile getById(String id) {

        return repo.findByBizId(id);
    }

    public InputStream download(StorageFile sf) throws IOException {

        StorageFile file = repo.findByBizId(sf.getBizId());

        return storage.download(file.getStoragePath());
    }

    @Override
    public void deleteAll(Collection<String> ids) {

        if (ids == null || ids.isEmpty()) {
            return;
        }

        // 收集所有存储路径
        Collection<String> storagePaths = repo.collectRemovePath(ids);
        if (storagePaths.isEmpty()) {
            return;
        }

        // 批量删除存储文件
        storage.deleteAll(storagePaths);

        // 批量删除数据库记录
        repo.removeAllByIds(ids);
    }

    private void validateFile(MultipartFile file) {

        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        if (file.getSize() > fileProperties.getMaxSize()) {
            throw new IllegalArgumentException("File size exceeds limit");
        }

        String   extension = getExtension(file.getOriginalFilename());
        String[] allowed   = fileProperties.getAllowedExtensions().split(",");
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
                category,
                now.getYear(),
                now.getMonthValue(),
                now.getDayOfMonth(),
                filename
        );
    }

    private final StorageFileToFileItemMapper mapper;

    public FileItem getItem(String value) {

        return mapper.convert(repo.findByBizId(value));
    }

    @Override
    public Map<String, FileItem> getItems(Collection<String> ids) {

        List<StorageFile> files = repo.findByIds(ids);
        return files.stream().collect(Collectors.toMap(
                StorageFile::getBizId,
                mapper::convert
        ));
    }

}