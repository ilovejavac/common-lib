package com.dev.lib.jpa.adapt;

import com.dev.lib.exceptions.BizException;
import com.dev.lib.local.task.message.domain.adapter.ILocalTaskMessagePort;
import com.dev.lib.local.task.message.domain.adapter.IRabbitPublish;
import com.dev.lib.local.task.message.domain.model.entity.TaskMessageEntityCommand;
import com.dev.lib.util.http.GenerichHttpGateway;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class LocalTaskMessagePortAdapt implements ILocalTaskMessagePort {
    private final IRabbitPublish rabbitPublish;
    private final GenerichHttpGateway httpGateway;
    private final ObjectMapper mapper;

    @Override
    public String notify2http(TaskMessageEntityCommand cmd) {
        TaskMessageEntityCommand.NotifyConfig.Http config =
                Optional.ofNullable(cmd.getNotifyConfig()).map(TaskMessageEntityCommand.NotifyConfig::getHttp)
                        .orElseThrow(() -> new BizException(50040, ""));

        GenerichHttpGateway http = GenerichHttpGateway.resolve(config);


        return "";
    }

    @Override
    public String notify2rabbit(TaskMessageEntityCommand cmd) {
        TaskMessageEntityCommand.NotifyConfig.Rabbit config =
                Optional.ofNullable(cmd.getNotifyConfig()).map(TaskMessageEntityCommand.NotifyConfig::getRabbit)
                        .orElseThrow(() -> new BizException(50041, ""));

        try {
            rabbitPublish.publish(
                    config.getExchange(),
                    config.getTopic(),
                    mapper.writeValueAsString(config.getPayload())
            );
        } catch (Exception e) {

        } finally {

        }

        return "";
    }
}
