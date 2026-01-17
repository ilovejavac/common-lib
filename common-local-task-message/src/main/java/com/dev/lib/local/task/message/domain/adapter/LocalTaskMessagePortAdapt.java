package com.dev.lib.local.task.message.domain.adapter;

import com.alibaba.fastjson2.JSON;
import com.dev.lib.exceptions.BizException;
import com.dev.lib.local.task.message.domain.model.entity.TaskMessageEntityCommand;
import com.dev.lib.mq.MQ;
import com.dev.lib.mq.MessageExtend;
import com.dev.lib.util.http.GenerichHttpGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class LocalTaskMessagePortAdapt implements ILocalTaskMessagePort {

    private final IRabbitPublish rabbitPublish;

    private final GenerichHttpGateway httpGateway;

    private final ILocalTaskMessageAdapt adapt;

    @Override
    public String notify2http(TaskMessageEntityCommand cmd) {

        try {
            TaskMessageEntityCommand.NotifyConfig.Http config =
                    Optional.ofNullable(cmd.getNotifyConfig()).map(TaskMessageEntityCommand.NotifyConfig::getHttp)
                            .orElseThrow(() -> new BizException(
                                    103001,
                                    "没有配置 http"
                            ));

            // TODO: 实现 HTTP 调用
            // GenerichHttpGateway http = GenerichHttpGateway.resolve(config);
            // String result = http.execute();
            adapt.updateTaskStatusToSuccess(cmd.getTaskId());
            return "success";
        } catch (Exception e) {
            adapt.updateTaskStatusToFailed(cmd.getTaskId());
            throw e;
        }
    }

    @Override
    public String notify2rabbit(TaskMessageEntityCommand cmd) {

        TaskMessageEntityCommand.NotifyConfig.Rabbit config =
                Optional.ofNullable(cmd.getNotifyConfig()).map(TaskMessageEntityCommand.NotifyConfig::getRabbit)
                        .orElseThrow(() -> new BizException(
                                103002,
                                "没有配置 rabbitmq"
                        ));

        try {
            rabbitPublish.publish(
                    config.getExchange(),
                    config.getTopic(),
                    JSON.toJSONString(config.getPayload())
            );
            adapt.updateTaskStatusToSuccess(cmd.getTaskId());
            return "success";
        } catch (Exception e) {
            adapt.updateTaskStatusToFailed(cmd.getTaskId());
            throw e;
        }
    }

    @Override
    public String notify2mq(TaskMessageEntityCommand cmd) {

        TaskMessageEntityCommand.NotifyConfig.Mq config =
                Optional.ofNullable(cmd.getNotifyConfig()).map(TaskMessageEntityCommand.NotifyConfig::getMq)
                        .orElseThrow(() -> new BizException(
                                103003,
                                "没有配置 mq"
                        ));

        try {
            MessageExtend<Object> message = MessageExtend.Companion.of(config.getPayload());
            config.getPayload().forEach((k, v) -> message.set(k, String.valueOf(v)));
            MQ.INSTANCE.publish(config.getDestination(), message);
            adapt.updateTaskStatusToSuccess(cmd.getTaskId());
            return "success";
        } catch (Exception e) {
            adapt.updateTaskStatusToFailed(cmd.getTaskId());
            throw e;
        }
    }

}
