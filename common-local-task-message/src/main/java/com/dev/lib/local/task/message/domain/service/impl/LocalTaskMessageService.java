package com.dev.lib.local.task.message.domain.service.impl;

import com.dev.lib.exceptions.BizException;
import com.dev.lib.local.task.message.domain.adapter.ILocalTaskMessageAdapt;
import com.dev.lib.local.task.message.domain.adapter.ILocalTaskMessageEvent;
import com.dev.lib.local.task.message.domain.model.entity.TaskMessageEntityCommand;
import com.dev.lib.local.task.message.domain.service.LocalTaskMessageHandleService;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LocalTaskMessageService implements LocalTaskMessageHandleService {

    @Resource
    private ILocalTaskMessageEvent event;

    @Resource
    private ILocalTaskMessageAdapt adapt;

    @Override
    public void handle(TaskMessageEntityCommand cmd) {

        try {
            adapt.saveMessage(cmd);
            event.publish(cmd);
        } catch (Exception e) {
            log.error("受理任务消息执行失败 {}", cmd, e);
            throw new BizException(50060, "受理任务消息执行失败");
        }
    }

}
