package com.dev.lib.local.task.message.domain.adapter.poller;

import com.dev.lib.local.task.message.data.LocalTaskMessagePo;
import com.dev.lib.local.task.message.data.LocalTaskStatus;
import com.dev.lib.local.task.message.data.TaskMessageRepository;
import com.dev.lib.local.task.message.poller.core.PollerContext;
import com.dev.lib.local.task.message.poller.core.PollerStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Poller 存储适配器
 * 将 Poller 接口适配到本地消息表存储
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LocalTaskPollerStorage implements PollerStorage {

    private final TaskMessageRepository repository;

    @Override
    public void save(PollerContext task, int houseNumber) {
        LocalTaskMessagePo po = new LocalTaskMessagePo();
        po.setTaskId(task.getId());
        po.setTaskType(task.getTaskType());
        po.setPayload(task.getPayload());
        po.setHouseNumber(houseNumber);
        po.setStatus(LocalTaskStatus.PENDING);
        po.setRetryCount(0);
        po.setNextRetryTime(LocalDateTime.now());

        repository.save(po);
    }

    @Override
    public List<PollerContext> fetchPending(String taskType, List<Integer> houseNumbers, Long lastId, int limit) {
        List<LocalTaskMessagePo> pos = repository.loadsByHouseNumber(houseNumbers, lastId, limit);

        LocalDateTime now = LocalDateTime.now();

        return pos.stream()
                .filter(po -> {
                    // 1. PENDING 状态的任务
                    if (po.getStatus() == LocalTaskStatus.PENDING) {
                        return po.getNextRetryTime() == null || !po.getNextRetryTime().isAfter(now);
                    }
                    // 2. PROCESSING 状态但已超时的任务（服务器重启恢复）
                    if (po.getStatus() == LocalTaskStatus.PROCESSING && po.getProcessedAt() != null) {
                        LocalDateTime timeoutTime = po.getProcessedAt().plusMinutes(po.getTimeoutMinutes());
                        return now.isAfter(timeoutTime);
                    }
                    return false;
                })
                .filter(po -> taskType == null || taskType.equals(po.getTaskType()))
                .map(this::toContext)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateToProcessing(String taskId) {
        return repository.loadByTaskId(taskId)
                .filter(po -> {
                    // CAS：只允许 PENDING 或超时的 PROCESSING 状态更新
                    if (po.getStatus() == LocalTaskStatus.PENDING) {
                        return true;
                    }
                    // 超时的 PROCESSING 任务也可以重新处理
                    if (po.getStatus() == LocalTaskStatus.PROCESSING && po.getProcessedAt() != null) {
                        LocalDateTime timeoutTime = po.getProcessedAt().plusMinutes(po.getTimeoutMinutes());
                        return LocalDateTime.now().isAfter(timeoutTime);
                    }
                    return false;
                })
                .map(po -> {
                    po.setStatus(LocalTaskStatus.PROCESSING);
                    po.setProcessedAt(LocalDateTime.now());  // 记录开始处理时间
                    return true;
                })
                .orElse(false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateToSuccess(String taskId) {
        repository.loadByTaskId(taskId).ifPresent(po -> {
            po.setStatus(LocalTaskStatus.SUCCESS);
            po.setProcessedAt(LocalDateTime.now());
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateToFailed(String taskId, String errorMessage, LocalDateTime nextRetryTime) {
        repository.loadByTaskId(taskId).ifPresent(po -> {
            po.setStatus(nextRetryTime != null ? LocalTaskStatus.PENDING : LocalTaskStatus.FAILED);
            po.setErrorMessage(errorMessage);
            po.setRetryCount(po.getRetryCount() + 1);
            po.setNextRetryTime(nextRetryTime);
        });
    }

    private PollerContext toContext(LocalTaskMessagePo po) {
        return new PollerContext(
                po.getTaskId(),
                po.getTaskType(),
                po.getPayload(),
                po.getRetryCount(),
                po.getErrorMessage()
        );
    }

}
