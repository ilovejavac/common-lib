package com.dev.lib.storage.domain.service.virtual.cleanup;

import com.dev.lib.storage.data.SysFileBizIdRepository;
import com.dev.lib.storage.domain.service.virtual.StorageServiceNameProvider;
import com.dev.lib.storage.domain.service.virtual.storage.VfsFileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * VFS 异步清理服务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VfsAsyncCleanupService {

    private final SysFileBizIdRepository sysFileRepository;

    private final VfsFileStorageService storageService;

    private final StorageServiceNameProvider serviceNameProvider;

    /**
     * 异步清理 COW 旧版本文件
     */
    @Async
    @Transactional(rollbackFor = Exception.class)
    public void cleanupOldVersions(String bizId) {

        sysFileRepository.findByBizIdForUpdate(bizId).ifPresent(file -> {
            if (!Objects.equals(serviceNameProvider.currentServiceName(), file.getServiceName())) {
                return;
            }
            if (file.getDeleteAfter() == null || file.getDeleteAfter().isAfter(LocalDateTime.now())) {
                return;
            }

            List<String> oldPaths = file.getOldStoragePaths();
            if (oldPaths == null || oldPaths.isEmpty()) {
                file.setDeleteAfter(null);
                sysFileRepository.save(file);
                return;
            }

            try {
                storageService.deleteAll(new ArrayList<>(oldPaths));
                file.setOldStoragePaths(null);
                file.setDeleteAfter(null);
                sysFileRepository.save(file);
            } catch (Exception e) {
                log.warn("Failed to cleanup old versions for file {}", file.getVirtualPath(), e);
            }
        });
    }

    /**
     * 异步清理过期临时文件
     */
    @Async
    @Transactional(rollbackFor = Exception.class)
    public void cleanupExpiredTemporaryFile(String bizId) {

        sysFileRepository.findByBizIdForUpdate(bizId).ifPresent(file -> {
            if (!Objects.equals(serviceNameProvider.currentServiceName(), file.getServiceName())) {
                return;
            }
            if (!Boolean.TRUE.equals(file.getTemporary())) {
                return;
            }
            if (file.getExpirationAt() == null || file.getExpirationAt().isAfter(LocalDateTime.now())) {
                return;
            }
            if (Boolean.TRUE.equals(file.getIsDirectory())) {
                return;
            }

            List<String> paths = storageService.collectStoragePaths(file);
            try {
                if (!paths.isEmpty()) {
                    storageService.deleteAll(paths);
                }
                sysFileRepository.delete(file);
            } catch (Exception e) {
                log.warn("Failed to cleanup expired temporary file {}", file.getVirtualPath(), e);
            }
        });
    }

}
