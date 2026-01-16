package com.dev.lib.storage.trigger.schedule;

import com.dev.lib.security.util.SecurityContextHolder;
import com.dev.lib.storage.data.SysFile;
import com.dev.lib.storage.data.SysFileRepository;
import com.dev.lib.storage.domain.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * VFS 文件清理任务
 * 定期清理标记为延迟删除的旧文件
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VfsCleanupTask {

    private final SysFileRepository fileRepository;

    private final StorageService storageService;

    /**
     * 每 5 分钟执行一次清理任务
     */
    @Scheduled(fixedRate = 300000) // 5 分钟
    @Transactional(rollbackFor = Exception.class)
    public void cleanupOldFiles() {

        try {
            LocalDateTime now = LocalDateTime.now();

            // 查询所有需要删除的旧文件
            List<SysFile> filesToClean = fileRepository.loads(new SysFileRepository.Query().setDeleteAfterLe(now));

            if (filesToClean.isEmpty()) {
                return;
            }

            log.info("VFS cleanup task: found {} old files to delete", filesToClean.size());

            SecurityContextHolder.withSystem(() -> {
                int successCount  = 0;
                int failCount     = 0;
                int totalOldFiles = 0;

                for (SysFile file : filesToClean) {
                    try {
                        List<String> oldPaths = file.getOldStoragePaths();
                        if (oldPaths != null && !oldPaths.isEmpty()) {
                            // 批量删除旧存储文件
                            storageService.deleteAll(oldPaths);
                            totalOldFiles += oldPaths.size();
                        }

                        // 清空旧路径和删除时间
                        file.setOldStoragePaths(null);
                        file.setDeleteAfter(null);

                        successCount++;
                    } catch (Exception e) {
                        log.error("Failed to delete old files for: {}", file.getVirtualPath(), e);
                        failCount++;
                    }
                }

                log.info(
                        "VFS cleanup task completed: {} files processed, {} old files deleted ({} succeeded, {} failed)",
                        filesToClean.size(), totalOldFiles, successCount, failCount
                );

            });

        } catch (Exception e) {
            log.error("VFS cleanup task failed", e);
        }
    }

}
