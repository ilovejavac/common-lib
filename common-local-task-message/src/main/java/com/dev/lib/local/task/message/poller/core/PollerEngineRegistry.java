package com.dev.lib.local.task.message.poller.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Poller 引擎注册中心
 * 管理多个任务类型的 PollerEngine 实例
 */
@Component
public class PollerEngineRegistry {

    private static final Logger log = LoggerFactory.getLogger(PollerEngineRegistry.class);

    private final Map<String, PollerEngine> engines = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private List<PollerEngine> pollerEngines;

    /**
     * 注册引擎
     *
     * @param engine PollerEngine 实例
     */
    public void register(PollerEngine engine) {
        engines.put(engine.getTaskType(), engine);
        engine.start();  // 注册后自动启动
        log.info("Registered and started PollerEngine for taskType: {}", engine.getTaskType());
    }

    /**
     * 获取指定任务类型的引擎
     *
     * @param taskType 任务类型
     * @return PollerEngine 实例，不存在返回 null
     */
    public PollerEngine getEngine(String taskType) {
        return engines.get(taskType);
    }

    /**
     * 提交任务到指定类型的引擎
     *
     * @param taskType   任务类型
     * @param businessId 业务ID
     * @param payload    任务数据
     * @return 任务ID
     * @throws IllegalArgumentException 任务类型不存在
     */
    public String submit(String taskType, String businessId, Map<String, Object> payload) {
        PollerEngine engine = getEngine(taskType);
        if (engine == null) {
            throw new IllegalArgumentException("No PollerEngine registered for taskType: " + taskType);
        }
        return engine.submit(businessId, payload);
    }

    /**
     * 启动所有已注册的引擎
     */
    public void startAll() {
        log.info("Starting all PollerEngines, total: {}", engines.size());
        engines.values().forEach(PollerEngine::start);
    }

    /**
     * 停止所有已注册的引擎
     */
    public void stopAll() {
        log.info("Stopping all PollerEngines");
        engines.values().forEach(PollerEngine::stop);
    }

    /**
     * 遍历所有引擎
     *
     * @param action 对每个引擎执行的操作
     */
    public void forEach(Consumer<PollerEngine> action) {
        engines.values().forEach(action);
    }

    /**
     * 获取所有已注册的任务类型
     *
     * @return 任务类型列表
     */
    public List<String> getRegisteredTaskTypes() {
        return List.copyOf(engines.keySet());
    }

}
