//package com.dev.lib.local.task.message.poller.core;
//
//import com.dev.lib.local.task.message.poller.processor.TaskProcessor;
//import com.dev.lib.local.task.message.poller.strategy.BackoffStrategy;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//
//import java.time.Duration;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//
//import static org.junit.jupiter.api.Assertions.*;
//
///**
// * PollerEngine 单元测试
// */
//class PollerEngineTest {
//
//    private TestPollerStorage mockStorage;
//    private List<TaskProcessor<?>> processors;
//    private PollerConfig config;
//
//    @BeforeEach
//    void setUp() {
//        mockStorage = new TestPollerStorage();
//        processors = new ArrayList<>();
//        config = PollerConfig.builder()
//            .shardCount(10)
//            .threadCount(2)
//            .pollInterval(Duration.ofMillis(100))
//            .fetchLimit(10)
//            .maxRetry(3)
//            .baseDelay(Duration.ofMillis(50))
//            .maxDelay(Duration.ofSeconds(1))
//            .backoffStrategy(BackoffStrategy.EXPONENTIAL)
//            .build();
//    }
//
//    @Test
//    void testSubmitTask() {
//        // 创建测试处理器
//        TestProcessor processor = new TestProcessor("TEST_TYPE");
//        processors.add(processor);
//
//        // 创建引擎
//        PollerEngine engine = PollerEngine.builder()
//            .storage(mockStorage)
//            .processors(processors)
//            .config(config)
//            .build();
//
//        // 提交任务
//        String taskId = engine.submit("task-123", "TEST_TYPE", Map.of("key", "value"));
//
//        assertEquals("task-123", taskId);
//        assertTrue(mockStorage.isSaved());
//        assertEquals("TEST_TYPE", mockStorage.getSavedTaskType());
//    }
//
//    @Test
//    void testSubmitTaskWithUnknownType() {
//        processors.add(new TestProcessor("TEST_TYPE"));
//
//        PollerEngine engine = PollerEngine.builder()
//            .storage(mockStorage)
//            .processors(processors)
//            .config(config)
//            .build();
//
//        assertThrows(IllegalArgumentException.class, () -> {
//            engine.submit("task-123", "UNKNOWN_TYPE", Map.of());
//        });
//    }
//
//    @Test
//    void testBackoffStrategies() {
//        // 测试固定延迟
//        Duration fixed = BackoffStrategy.FIXED.calculateNextDelay(
//            Duration.ofSeconds(1), 5, Duration.ofMinutes(5)
//        );
//        assertEquals(Duration.ofSeconds(1), fixed);
//
//        // 测试线性增长
//        Duration linear = BackoffStrategy.LINEAR.calculateNextDelay(
//            Duration.ofSeconds(1), 3, Duration.ofMinutes(5)
//        );
//        assertEquals(Duration.ofSeconds(3), linear);
//
//        // 测试指数增长
//        Duration exponential = BackoffStrategy.EXPONENTIAL.calculateNextDelay(
//            Duration.ofSeconds(1), 4, Duration.ofMinutes(5)
//        );
//        assertEquals(Duration.ofSeconds(8), exponential);
//
//        // 测试最大延迟限制
//        Duration capped = BackoffStrategy.EXPONENTIAL.calculateNextDelay(
//            Duration.ofSeconds(10), 10, Duration.ofSeconds(30)
//        );
//        assertEquals(Duration.ofSeconds(30), capped);
//    }
//
//    /**
//     * 测试用的处理器实现
//     */
//    static class TestProcessor implements TaskProcessor<String> {
//        private final String taskType;
//
//        TestProcessor(String taskType) {
//            this.taskType = taskType;
//        }
//
//        @Override
//        public String execute(PollerContext context) {
//            return "success";
//        }
//
//        @Override
//        public String getTaskType() {
//            return taskType;
//        }
//    }
//
//    /**
//     * 测试用的存储实现
//     */
//    static class TestPollerStorage implements PollerStorage {
//        private boolean saved;
//        private String savedTaskType;
//
//        @Override
//        public void save(String id, String taskType, Object payload, int shardId) {
//            this.saved = true;
//            this.savedTaskType = taskType;
//        }
//
//        @Override
//        public List<TaskSnapshot> fetchPending(String taskType, int shardId, int limit) {
//            return List.of();
//        }
//
//        @Override
//        public boolean updateToProcessing(String id) {
//            return true;
//        }
//
//        @Override
//        public void updateToSuccess(String id) {
//        }
//
//        @Override
//        public void updateToFailed(String id, String errorMessage) {
//        }
//
//        @Override
//        public PollerContext loadContext(String taskId) {
//            return new PollerContext(taskId, "TEST_TYPE", Map.of(), 0, null);
//        }
//
//        public boolean isSaved() {
//            return saved;
//        }
//
//        public String getSavedTaskType() {
//            return savedTaskType;
//        }
//    }
//
//}
