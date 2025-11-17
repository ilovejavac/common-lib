package com.dev.lib.entity.id;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SnowflakeConfig {

    private static final String LOCK_KEY_PREFIX = "snowflake:worker:";
    private static final int MAX_RETRY_TIMES = 3;
    private static final long RETRY_DELAY_MS = 1000L;

    private static volatile SnowflakeConfig instance;

    //    private final RedissonClient redissonClient;
    private SnowflakeDistributeId snowflakeWorker;

    private long acquiredWorkerId = -1;
    private long acquiredDatacenterId = -1;

    public static SnowflakeDistributeId getWorker() {
        if (instance == null || instance.snowflakeWorker == null) {
            throw new IllegalStateException("SnowflakeConfig not initialized yet");
        }
        return instance.snowflakeWorker;
    }

    @PostConstruct
    public void init() {
        log.info("=".repeat(60));
        log.info("Initializing Snowflake");
        log.info("=".repeat(60));

        try {
            long datacenterId = getDatacenterId();
            long workerId = acquireWorkerId(datacenterId);

            acquiredWorkerId = workerId;
            acquiredDatacenterId = datacenterId;

            String twepochStr = System.getenv("SNOWFLAKE_EPOCH");
            long twepoch = (twepochStr != null) ? Long.parseLong(twepochStr) : 1542448800000L;

            snowflakeWorker = SnowflakeDistributeId.getInstance(workerId, datacenterId, twepoch);
            instance = this;

            log.info("✓ Snowflake initialized!");
            log.info("  WorkerId: {}", workerId);
            log.info("  DatacenterId: {}", datacenterId);
            log.info("=".repeat(60));

        } catch (Exception e) {
            log.error("✗ Failed to initialize Snowflake!", e);
            throw new RuntimeException("Snowflake initialization failed", e);
        }
    }

    private long getDatacenterId() {
        String datacenterIdStr = System.getenv("DATACENTER_ID");
        long datacenterId = (datacenterIdStr != null) ? Long.parseLong(datacenterIdStr) : 0L;
        return Math.max(0, Math.min(datacenterId, 15));
    }

    private long acquireWorkerId(long datacenterId) {
//        Exception lastException = null;
//
//        for (int attempt = 0; attempt < MAX_RETRY_TIMES; attempt++) {
//            try {
//                for (long workerId = 0; workerId <= 15; workerId++) {
//                    String lockKey = LOCK_KEY_PREFIX + datacenterId + ":" + workerId;
//                    RLock lock = redissonClient.getLock(lockKey);
//
//                    if (lock.tryLock(0, TimeUnit.SECONDS)) {
//                        workerLock = lock;
//                        log.info("✓ Acquired workerId={} (attempt {}/{})", workerId, attempt + 1, MAX_RETRY_TIMES);
//                        return workerId;
//                    }
//                }
//
//                log.warn("All worker IDs occupied (attempt {}/{})", attempt + 1, MAX_RETRY_TIMES);
//
//            } catch (Exception e) {
//                lastException = e;
//                log.error("Failed to acquire workerId (attempt {}/{})", attempt + 1, MAX_RETRY_TIMES, e);
//            }
//
//            if (attempt < MAX_RETRY_TIMES - 1) {
//                try {
//                    Thread.sleep(RETRY_DELAY_MS);
//                } catch (InterruptedException ie) {
//                    Thread.currentThread().interrupt();
//                    throw new RuntimeException("Interrupted while retrying", ie);
//                }
//            }
//        }
//
//        throw new RuntimeException("Failed to acquire worker ID after " + MAX_RETRY_TIMES + " attempts", lastException);
        return 0;
    }

    @PreDestroy
    public void destroy() {
        log.info("Shutting down Snowflake, releasing workerId...");
//
//        try {
//            if (workerLock != null && workerLock.isHeldByCurrentThread()) {
//                workerLock.unlock();
//                log.info("✓ Released workerId={}", acquiredWorkerId);
//            }
//        } catch (Exception e) {
//            log.error("Failed to release workerId", e);
//        }

        instance = null;
    }
}