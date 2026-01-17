package com.dev.lib.local.task.message.storage;

import com.dev.lib.local.task.message.data.TaskMessageRepository;
import com.dev.lib.local.task.message.domain.adapter.ILocalTaskMessageAdapt;
import com.dev.lib.local.task.message.domain.model.NotifyType;
import com.dev.lib.local.task.message.domain.model.entity.TaskMessageEntityCommand;
import com.dev.lib.mq.MessageExtend;
import com.dev.lib.mq.reliability.MessageStorage;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class LocalTaskMessageStorage implements MessageStorage {

    private final ILocalTaskMessageAdapt adapt;
    private final TaskMessageRepository repository;

    public LocalTaskMessageStorage(ILocalTaskMessageAdapt adapt, TaskMessageRepository repository) {
        this.adapt = adapt;
        this.repository = repository;
    }

    @Override
    public void saveBlocking(MessageExtend<?> message) {
        String destination = (String) message.getHeaders().getOrDefault("destination", "default");
        TaskMessageEntityCommand cmd = createCommand(message, destination);
        adapt.saveMessage(cmd);
    }

    @Override
    public void markAsConsumedBlocking(String messageId) {
        adapt.updateTaskStatusToSuccess(messageId);
    }

    @Override
    public List<MessageExtend<?>> getPendingMessagesBlocking(int limit) {
        List<Integer> houseNumbers = java.util.stream.IntStream.range(0, 10)
                .boxed()
                .collect(Collectors.toList());
        List<TaskMessageEntityCommand> cmdList = adapt.selectByHouseNumber(houseNumbers, "", limit);

        return cmdList.stream()
                .map(cmd -> {
                    TaskMessageEntityCommand.NotifyConfig config = cmd.getNotifyConfig();
                    if (config != null && config.getMq() != null) {
                        return (MessageExtend<?>) config.getMq().getPayload().get("message");
                    }
                    return null;
                })
                .filter(msg -> msg != null)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteBlocking(String messageId) {
        TaskMessageRepository.Query query = new TaskMessageRepository.Query();
        query.setTaskId(messageId);
        repository.physicalDelete().delete(query);
    }

    public void saveAsPending(MessageExtend<?> message, String destination) {
        TaskMessageEntityCommand cmd = createCommand(message, destination);
        adapt.saveMessage(cmd);
    }

    private TaskMessageEntityCommand createCommand(MessageExtend<?> message, String destination) {
        TaskMessageEntityCommand cmd = new TaskMessageEntityCommand();
        cmd.setTaskId(message.getId().toString());
        cmd.setTaskName((String) message.getHeaders().getOrDefault("task-name", "mq-task"));
        cmd.setBusinessId(message.getId().toString());
        cmd.setMaxRetry(message.getRetry());
        cmd.setNotifyType(NotifyType.MQ);

        TaskMessageEntityCommand.NotifyConfig notifyConfig = new TaskMessageEntityCommand.NotifyConfig();
        TaskMessageEntityCommand.NotifyConfig.Mq mqConfig = new TaskMessageEntityCommand.NotifyConfig.Mq();
        mqConfig.setDestination(destination);
        mqConfig.setPayload(Map.of(
                "message", message,
                "body", message.getBody(),
                "headers", message.getHeaders(),
                "key", message.getKey()
        ));
        notifyConfig.setMq(mqConfig);
        cmd.setNotifyConfig(notifyConfig);

        return cmd;
    }
}
