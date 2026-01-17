package com.dev.lib.local.task.message.domain.service.impl;

import com.dev.lib.local.task.message.data.LocalTaskStatus;
import com.dev.lib.local.task.message.domain.adapter.ILocalTaskMessageAdapt;
import com.dev.lib.local.task.message.domain.adapter.ILocalTaskMessagePort;
import com.dev.lib.local.task.message.domain.model.entity.TaskMessageEntityCommand;
import com.dev.lib.local.task.message.domain.service.LocalTaskMessageHandleService;
import com.dev.lib.local.task.message.domain.service.ITaskNotifyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LocalTaskMessageHandleServiceImpl implements LocalTaskMessageHandleService {

    private final ILocalTaskMessagePort messagePort;
    private final ILocalTaskMessageAdapt messageAdapt;
    private final ITaskNotifyService notifyService;

    @Override
    public void handle(TaskMessageEntityCommand cmd) {
        String taskId = cmd.getTaskId();

        try {
            messageAdapt.updateTaskStatusToProcessing(taskId);

            String result = notifyService.notify(cmd);
            log.info("任务 [{}] 通知成功: {}", taskId, result);

            messageAdapt.updateTaskStatusToSuccess(taskId);
        } catch (Exception e) {
            log.error("任务 [{}] 处理失败: {}", taskId, e.getMessage(), e);

            int retryCount = cmd.getRetryCount() + 1;
            if (retryCount >= cmd.getMaxRetry()) {
                log.warn("任务 [{}] 达到最大重试次数 [{}]，标记为失败", taskId, cmd.getMaxRetry());
                messageAdapt.updateTaskStatusToFailed(taskId);
            } else {
                log.info("任务 [{}] 将进行第 [{}] 次重试", taskId, retryCount);
            }
        }
    }
}
