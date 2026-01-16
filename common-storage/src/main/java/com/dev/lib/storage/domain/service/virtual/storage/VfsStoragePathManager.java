package com.dev.lib.storage.domain.service.virtual.storage;

import com.dev.lib.entity.id.IDWorker;
import com.dev.lib.storage.config.AppStorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * VFS 存储路径管理器
 * 负责生成存储路径和存储文件名
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VfsStoragePathManager {

    private static final String DEFAULT_STORAGE_PREFIX = "vfs";

    private final AppStorageProperties storageProperties;

    public AppStorageProperties getStorageProperties() {

        return storageProperties;
    }

    /**
     * 生成存储路径
     *
     * @param fileName          文件名
     * @param storagePathPrefix 自定义存储路径前缀，为 null 时使用默认的 vfs/年/月/日/
     * @return 存储路径
     */
    public String generateStoragePath(String fileName, String storagePathPrefix) {

        String storageName = generateStorageName(fileName);

        if (storagePathPrefix != null && !storagePathPrefix.isEmpty()) {
            return storagePathPrefix.endsWith("/")
                   ? storagePathPrefix + storageName
                   : storagePathPrefix + "/" + storageName;
        }

        LocalDate now = LocalDate.now();
        return String.format(
                "%s/%d/%02d/%02d/%s",
                DEFAULT_STORAGE_PREFIX,
                now.getYear(),
                now.getMonthValue(),
                now.getDayOfMonth(),
                storageName
        );
    }

    /**
     * 生成存储文件名（带扩展名）
     *
     * @param fileName 原始文件名
     * @return 存储文件名
     */
    private String generateStorageName(String fileName) {

        String storageName = IDWorker.newId();
        int    dotIdx      = fileName.lastIndexOf('.');
        if (dotIdx > 0) {
            storageName += fileName.substring(dotIdx);
        }
        return storageName;
    }

}
