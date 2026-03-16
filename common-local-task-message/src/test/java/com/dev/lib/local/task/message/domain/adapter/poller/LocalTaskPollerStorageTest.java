package com.dev.lib.local.task.message.domain.adapter.poller;

import com.dev.lib.local.task.message.data.LocalTaskMessagePo;
import com.dev.lib.local.task.message.data.TaskMessageRepository;
import com.dev.lib.local.task.message.poller.core.PollerContext;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocalTaskPollerStorageTest {

    @Test
    void savePersistsConfiguredMaxRetryAndTimeout() throws Exception {
        CapturedPo captured = new CapturedPo();
        TaskMessageRepository repository = (TaskMessageRepository) Proxy.newProxyInstance(
                TaskMessageRepository.class.getClassLoader(),
                new Class[]{TaskMessageRepository.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "save" -> {
                            captured.po = (LocalTaskMessagePo) args[0];
                            return captured.po;
                        }
                        case "loadsDuePendingByHouseNumber", "loadsProcessingByHouseNumber" -> {
                            return List.of();
                        }
                        case "loadByTaskId", "load", "loadById" -> {
                            return Optional.empty();
                        }
                        case "lockForUpdate" -> {
                            return proxy;
                        }
                        default -> {
                            return null;
                        }
                    }
                }
        );

        LocalTaskPollerStorage storage = new LocalTaskPollerStorage(repository);
        Field serviceName = LocalTaskPollerStorage.class.getDeclaredField("serviceName");
        serviceName.setAccessible(true);
        serviceName.set(storage, "task-server");

        PollerContext context = new PollerContext();
        context.setId("task-1");
        context.setTaskType("poller_ai-model.train");
        context.setPayload(Map.of("jobid", "job-1"));
        context.setMaxRetry(20);
        context.setTimeoutMinutes(9);

        storage.save(context, 2);

        assertEquals(20, captured.po.getMaxRetry());
        assertEquals(9, captured.po.getTimeoutMinutes());
        assertEquals("task-server", captured.po.getServiceName());
    }

    private static class CapturedPo {
        private LocalTaskMessagePo po;
    }
}
