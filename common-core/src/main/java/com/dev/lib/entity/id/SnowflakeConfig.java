package com.dev.lib.entity.id;

import com.dev.lib.config.properties.AppSnowFlakeProperties;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class SnowflakeConfig implements InitializingBean {

    private final AppSnowFlakeProperties snowFlakeProperties;

    private static final String LOCK_KEY_PREFIX = "snowflake:worker:";

    private static final int MAX_RETRY_TIMES = 3;

    private static final long RETRY_DELAY_MS = 1000L;

    //    private final RedissonClient redissonClient;
    @Getter
    private static SnowflakeDistributeId worker;

    private long acquiredWorkerId = -1;

    private long acquiredDatacenterId = -1;

    @Override
    public void afterPropertiesSet() throws Exception {

        log.info("=".repeat(60));
        log.info("Initializing Snowflake");
        log.info("=".repeat(60));

        try {
            long datacenterId = getDatacenterId();
            long workerId     = acquireWorkerId(datacenterId);

            acquiredWorkerId = workerId;
            acquiredDatacenterId = datacenterId;

            worker = SnowflakeDistributeId.getInstance(workerId, datacenterId);

            log.info("✓ Snowflake initialized!");
            log.info("  WorkerId: {}", workerId);
            log.info("  DatacenterId: {}", datacenterId);
        } catch (Exception e) {
            log.error("✗ Failed to initialize Snowflake!", e);
            throw new RuntimeException("Snowflake initialization failed", e);
        }
    }

    private long getDatacenterId() {

        return Math.clamp(Optional.ofNullable(snowFlakeProperties.getDataCenterId()).orElse(0), 0, 15);
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
    }

}