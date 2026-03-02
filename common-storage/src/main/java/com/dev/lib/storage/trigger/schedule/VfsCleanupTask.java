package com.dev.lib.storage.trigger.schedule;

import com.dev.lib.storage.data.SysFile;
import com.dev.lib.storage.data.SysFileRepository;
import com.dev.lib.storage.domain.service.virtual.StorageServiceNameProvider;
import com.dev.lib.storage.domain.service.virtual.cleanup.VfsAsyncCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * VFS 清理调度任务：
 * 1. 触发旧版本文件异步清理
 * 2. 触发过期临时文件异步清理
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VfsCleanupTask {

    private final SysFileRepository      sysFileRepository;

    private final VfsAsyncCleanupService asyncCleanupService;

    private final StorageServiceNameProvider serviceNameProvider;

    @Scheduled(
            fixedDelayString = "${app.storage.vfs.cleanup-interval-ms:300000}",
            initialDelayString = "${app.storage.vfs.cleanup-initial-delay-ms:60000}"
    )
    public void scheduleCleanup() {

        LocalDateTime now = LocalDateTime.now();
        String serviceName = serviceNameProvider.currentServiceName();

        List<SysFile> oldVersionFiles = sysFileRepository.loads(
                new SysFileRepository.Query()
                        .setServiceName(serviceName)
                        .setDeleteAfterLe(now)
        );

        List<SysFile> expiredTempFiles = sysFileRepository.loads(
                new SysFileRepository.Query()
                        .setServiceName(serviceName)
                        .setTemporary(true)
                        .setExpirationAtLe(now)
        );

        if (oldVersionFiles.isEmpty() && expiredTempFiles.isEmpty()) {
            return;
        }

        log.debug(
                "VFS cleanup trigger: oldVersionFiles={}, expiredTempFiles={}",
                oldVersionFiles.size(),
                expiredTempFiles.size()
        );

        for (SysFile file : oldVersionFiles) {
            asyncCleanupService.cleanupOldVersions(file.getBizId());
        }
        for (SysFile file : expiredTempFiles) {
            asyncCleanupService.cleanupExpiredTemporaryFile(file.getBizId());
        }
    }

}
