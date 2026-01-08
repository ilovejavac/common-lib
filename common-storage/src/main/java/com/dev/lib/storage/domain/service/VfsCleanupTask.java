package com.dev.lib.storage.domain.service;

import com.dev.lib.security.util.SecurityContextHolder;
import com.dev.lib.storage.data.SysFile;
import com.dev.lib.storage.data.SysFileRepository;
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
            List<SysFile> filesToClean = fileRepository.loads(new SysFileRepository.Query().setTemporary(true));

            if (filesToClean.isEmpty()) {
                return;
            }

            log.info("VFS cleanup task: found {} old files to delete", filesToClean.size());

            SecurityContextHolder.withSystem(() -> {
                int successCount = 0;
                int failCount    = 0;

                for (SysFile file : filesToClean) {
                    try {
                        // 删除存储层的旧文件
                        storageService.delete(file.getOldStoragePath());

                        // 清除数据库中的标记
                        fileRepository.delete(file);

                        successCount++;
                    } catch (Exception e) {
                        log.error("Failed to delete old file: {}", file.getOldStoragePath(), e);
                        failCount++;
                    }
                }

                log.info("VFS cleanup task completed: {} succeeded, {} failed", successCount, failCount);

            });

        } catch (Exception e) {
            log.error("VFS cleanup task failed", e);
        }
    }

}
