package com.dev.lib.local.task.message.domain.adapter.poller;

import com.dev.lib.local.task.message.data.LocalTaskMessagePo;
import com.dev.lib.local.task.message.data.LocalTaskStatus;
import com.dev.lib.local.task.message.data.TaskMessageRepository;
import com.dev.lib.local.task.message.poller.core.PollerContext;
import com.dev.lib.local.task.message.poller.core.PollerStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
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

    @Value("${spring.application.name:unknown}")
    private String serviceName;

    @Override
    public void save(PollerContext task, int houseNumber) {
        LocalTaskMessagePo po = new LocalTaskMessagePo();
        po.setTaskId(task.getId());
        po.setTaskType(task.getTaskType());
        po.setTaskName(task.getTaskName());
        po.setServiceName(serviceName);
        po.setPayload(task.getPayload());
        po.setHouseNumber(houseNumber);
        po.setStatus(LocalTaskStatus.PENDING);
        po.setRetryCount(0);
        po.setNextRetryTime(LocalDateTime.now());
        po.setTimeoutMinutes(task.getTimeoutMinutes() != null ? task.getTimeoutMinutes() : 5);

        repository.save(po);
    }

    @Override
    public List<PollerContext> fetchPending(String taskType, List<Integer> houseNumbers, Long lastId, int limit) {
        LocalDateTime now = LocalDateTime.now();
        List<LocalTaskMessagePo> pos = new ArrayList<>(
                repository.loadsDuePendingByHouseNumber(houseNumbers, lastId, limit, taskType, now)
        );

        int remaining = limit - pos.size();
        if (remaining > 0) {
            // PROCESSING 任务用于超时恢复，先多拉一批再在内存中过滤
            int candidateLimit = Math.max(remaining * 5, remaining);
            List<LocalTaskMessagePo> processingCandidates =
                    repository.loadsProcessingByHouseNumber(houseNumbers, lastId, candidateLimit, taskType);

            pos.addAll(
                    processingCandidates.stream()
                            .filter(po -> isProcessingTimeout(po, now))
                            .limit(remaining)
                            .toList()
            );
        }

        return pos.stream()
                .map(this::toContext)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateToProcessing(String taskId) {
        return repository.lockForUpdate().load(new TaskMessageRepository.Query().setTaskId(taskId))
                .map(po -> {
                    LocalDateTime now = LocalDateTime.now();

                    if (po.getStatus() == LocalTaskStatus.PENDING) {
                        if (po.getNextRetryTime() != null && po.getNextRetryTime().isAfter(now)) {
                            return false;
                        }
                    } else if (po.getStatus() == LocalTaskStatus.PROCESSING) {
                        if (!isProcessingTimeout(po, now)) {
                            return false;
                        }
                    } else {
                        return false;
                    }

                    po.setStatus(LocalTaskStatus.PROCESSING);
                    po.setProcessedAt(now);
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

    private boolean isProcessingTimeout(LocalTaskMessagePo po, LocalDateTime now) {
        if (po.getStatus() != LocalTaskStatus.PROCESSING || po.getProcessedAt() == null) {
            return false;
        }
        LocalDateTime timeoutTime = po.getProcessedAt().plusMinutes(po.getTimeoutMinutes());
        return now.isAfter(timeoutTime);
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
