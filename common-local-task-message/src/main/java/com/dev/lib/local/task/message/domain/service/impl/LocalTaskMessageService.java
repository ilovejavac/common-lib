package com.dev.lib.local.task.message.domain.service.impl;

import com.dev.lib.local.task.message.domain.adapter.ILocalTaskMessageEvent;
import com.dev.lib.local.task.message.domain.adapter.ILocalTaskMessagePort;
import com.dev.lib.local.task.message.domain.model.NotifyType;
import com.dev.lib.local.task.message.domain.model.entity.TaskMessageEntityCommand;
import com.dev.lib.local.task.message.domain.service.LocalTaskMessageHandleService;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Component
@RequiredArgsConstructor
public class LocalTaskMessageService implements LocalTaskMessageHandleService, InitializingBean {
    private final ILocalTaskMessagePort messagePort;
    private final Map<NotifyType, Function<TaskMessageEntityCommand, String>> notifyStrategy =
            new EnumMap<>(NotifyType.class);

    @Resource
    private ILocalTaskMessageEvent event;

    @Override
    public void handle(TaskMessageEntityCommand cmd) {
        try {
            event.saveMessage(cmd);
            event.publish(cmd);
            notify(cmd);
        } finally {

        }
    }

    private void notify(TaskMessageEntityCommand command) {
        try {
            Function<TaskMessageEntityCommand, String> notifier =
                    notifyStrategy.get(command.getNotifyType());
            if (notifier != null) {
                notifier.apply(command);
            }
        } finally {

        }
    }

    @Override
    public void afterPropertiesSet() {
        notifyStrategy.put(NotifyType.HTTP, messagePort::notify2http);
        notifyStrategy.put(NotifyType.RABBIT, messagePort::notify2rabbit);
    }
}
