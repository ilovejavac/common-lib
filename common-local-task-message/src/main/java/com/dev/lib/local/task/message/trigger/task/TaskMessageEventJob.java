package com.dev.lib.local.task.message.trigger.task;

import com.dev.lib.local.task.message.config.LocalTaskConfigProperties;
import com.dev.lib.local.task.message.domain.model.entity.TaskMessageEntityCommand;
import com.dev.lib.local.task.message.domain.service.ILocalTaskDataService;
import com.dev.lib.local.task.message.domain.service.ITaskNotifyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskMessageEventJob implements InitializingBean {
    private final Map<String, String> groupLastIdMap = new ConcurrentHashMap<>();

    private final ITaskNotifyService notifyService;
    private final ILocalTaskDataService service;
    private final ThreadPoolTaskScheduler scheduler;
    private final LocalTaskConfigProperties properties;

    @Override
    public void afterPropertiesSet() {
        List<LocalTaskConfigProperties.TaskGroupConfig> groups = properties.getGroupConfigs();
        if (groups == null || groups.isEmpty()) {
            log.info("TaskMessageEventJob 未配置任务组，跳过调度初始化");
            return;
        }

        for (LocalTaskConfigProperties.TaskGroupConfig group : groups) {
            scheduleGroup(group);
        }

    }

    private void scheduleGroup(LocalTaskConfigProperties.TaskGroupConfig group) {
        String groupId = group.getGroup();
        List<Integer> houseNumbers = group.getHouseNumbers();
        if (houseNumbers == null || houseNumbers.isEmpty()) {
            log.warn("任务组 [{}] 未配置 houseNumbers，跳过该组调度", groupId);
            return;
        }

        // 初始化 lastId
        groupLastIdMap.computeIfAbsent(
                groupId, k -> {
                    return service.selectMinIdByHouseNumber(houseNumbers);
                }
        );

        Runnable task = () -> {
            try {
                String lastId = groupLastIdMap.get(groupId);
                List<TaskMessageEntityCommand> cmdList =
                        service.selectByHouseNumber(houseNumbers, lastId, group.getLimit());
                if (cmdList == null || cmdList.isEmpty()) {
                    return;
                }

                // 发布事件
                for (TaskMessageEntityCommand cmd : cmdList) {
                    notifyService.notify(cmd);
                }

                // 更新 lastId
                String maxId = cmdList.stream().map(TaskMessageEntityCommand::getTaskId).max(Comparator.naturalOrder())
                        .orElse(lastId);
                groupLastIdMap.put(groupId, maxId);

                log.info("任务组 [{}] 处理完成：拉取{}条，lastId: {} -> {}", groupId, cmdList.size(), lastId, maxId);
            } catch (Exception e) {
                log.error("任务组 [{}] 执行异常: {}", groupId, e.getMessage(), e);
            }
        };

        if (group.getCron() != null && !group.getCron().trim().isEmpty()) {
            scheduler.schedule(task, new CronTrigger(group.getCron()));
            log.info("任务组 [{}] 已按 cron [{}] 调度", groupId, group.getCron());
        } else {
            long delay = group.getFixedDelayMs() != null ? group.getFixedDelayMs() : 5000L;
            scheduler.scheduleWithFixedDelay(task, Duration.ofMillis(delay));
            log.info("任务组 [{}] 已按 fixedDelayMs [{}] 调度", groupId, delay);
        }
    }

}
