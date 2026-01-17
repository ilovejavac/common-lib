package com.dev.lib.local.task.message.domain.service.impl;

import com.dev.lib.exceptions.BizException;
import com.dev.lib.local.task.message.domain.adapter.ILocalTaskMessagePort;
import com.dev.lib.local.task.message.domain.model.NotifyType;
import com.dev.lib.local.task.message.domain.model.entity.TaskMessageEntityCommand;
import com.dev.lib.local.task.message.domain.service.ITaskNotifyService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class TaskNotifyService implements ITaskNotifyService, InitializingBean {

    private final ILocalTaskMessagePort messagePort;

    private final Map<NotifyType, Function<TaskMessageEntityCommand, String>> notifyStrategy =
            new EnumMap<>(NotifyType.class);

    public String notify(TaskMessageEntityCommand command) {

        Function<TaskMessageEntityCommand, String> notifier =
                notifyStrategy.get(command.getNotifyType());
        if (notifier != null) {
            return notifier.apply(command);
        }

        throw new BizException(103004, "消息通知处理器不存在");
    }

    @Override
    public void afterPropertiesSet() {

        notifyStrategy.put(NotifyType.HTTP, messagePort::notify2http);
        notifyStrategy.put(NotifyType.RABBIT, messagePort::notify2rabbit);
        notifyStrategy.put(NotifyType.MQ, messagePort::notify2mq);
    }

}
