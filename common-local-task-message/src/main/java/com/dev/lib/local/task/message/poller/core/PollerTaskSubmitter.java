package com.dev.lib.local.task.message.poller.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Poller 任务提交器
 * 业务模块通过此类提交任务到本地消息表
 *
 * 使用示例：
 * <pre>
 * {@code
 * @Autowired
 * private PollerTaskSubmitter submitter;
 *
 * // 提交 RabbitMQ 重试任务
 * Map<String, Object> payload = Map.of(
 *     "destination", "order.exchange",
 *     "routingKey", "order.created",
 *     "body", message,
 *     "persistent", true
 * );
 * submitter.submit("RABBIT_RETRY", orderId, payload);
 * }
 * </pre>
 */
@Component
public class PollerTaskSubmitter {

    private final PollerEngineRegistry registry;

    @Autowired
    public PollerTaskSubmitter(PollerEngineRegistry registry) {
        this.registry = registry;
    }

    /**
     * 提交任务（自动生成任务ID）
     *
     * @param taskType   任务类型（如 "RABBIT_RETRY", "HTTP_NOTIFY"）
     * @param businessId 业务ID（用于计算门牌号和幂等）
     * @param payload    任务数据
     * @return 任务ID
     * @throws IllegalArgumentException 任务类型不存在
     */
    public String submit(String taskType, String businessId, Map<String, Object> payload) {
        return registry.submit(taskType, businessId, payload);
    }

    /**
     * 提交任务（指定任务ID）
     *
     * @param taskType   任务类型
     * @param taskId     任务ID
     * @param businessId 业务ID
     * @param payload    任务数据
     * @return 任务ID
     */
    public String submit(String taskType, String taskId, String businessId, Map<String, Object> payload) {
        PollerEngine engine = registry.getEngine(taskType);
        if (engine == null) {
            throw new IllegalArgumentException("No PollerEngine registered for taskType: " + taskType);
        }
        return engine.submit(taskId, businessId, payload);
    }

}
