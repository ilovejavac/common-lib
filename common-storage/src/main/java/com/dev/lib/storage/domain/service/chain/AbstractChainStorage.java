package com.dev.lib.storage.domain.service.chain;

import com.dev.lib.storage.config.AppStorageProperties;
import com.dev.lib.storage.data.SysFile;
import com.dev.lib.storage.data.SysFileObjectRepository;
import com.dev.lib.storage.domain.service.StorageServiceNameProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * ChainStorage 抽象基类。
 * 负责 bucket/objectKey 维度的文件元数据同步。
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractChainStorage {

    protected final AppStorageProperties fileProperties;

    protected final SysFileObjectRepository fileRepository;

    protected final StorageServiceNameProvider serviceNameProvider;

    protected SysFile saveFileRecord(String bucketName, String objectKey, String storagePath, Long size) {

        String serviceName = serviceNameProvider.currentServiceName();
        Optional<SysFile> existing = fileRepository.findByBucketNameAndObjectKeyForUpdate(
                serviceName,
                bucketName,
                objectKey
        );

        SysFile file = existing.orElseGet(SysFile::new);
        file.setServiceName(serviceName);
        file.setBucketName(bucketName);
        file.setObjectKey(objectKey);
        file.setStoragePath(storagePath);
        file.setStorageName(extractFileName(objectKey));
        file.setOriginalName(extractFileName(objectKey));
        file.setStorageType(fileProperties.getEffectiveType());
        file.setExtension(extractExtension(objectKey));

        if (size != null) {
            file.setSize(size);
        }

        return fileRepository.save(file);
    }

    protected String saveFileRecord(String bucketName, String objectKey, Long size) {

        return saveFileRecord(bucketName, objectKey, bucketName + "/" + objectKey, size).getBizId();
    }

    protected String updateFileRecord(String bucketName, String objectKey, String storagePath, long newSize) {

        String serviceName = serviceNameProvider.currentServiceName();
        Optional<SysFile> existing = fileRepository.findByBucketNameAndObjectKey(
                serviceName,
                bucketName,
                objectKey
        );
        if (existing.isPresent()) {
            SysFile file = existing.get();
            file.setStoragePath(storagePath);
            file.setSize(newSize);
            return fileRepository.save(file).getBizId();
        }
        return saveFileRecord(bucketName, objectKey, storagePath, newSize).getBizId();
    }

    protected void deleteFileRecord(String bucketName, String objectKey) {

        fileRepository.findByBucketNameAndObjectKey(
                        serviceNameProvider.currentServiceName(),
                        bucketName,
                        objectKey
                )
                .ifPresent(fileRepository::delete);
    }

    protected String extractFileName(String objectKey) {

        int lastSlash = objectKey.lastIndexOf('/');
        if (lastSlash >= 0) {
            return objectKey.substring(lastSlash + 1);
        }
        return objectKey;
    }

    protected String extractExtension(String objectKey) {

        String fileName = extractFileName(objectKey);
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            return fileName.substring(lastDot + 1).toLowerCase();
        }
        return null;
    }
}
