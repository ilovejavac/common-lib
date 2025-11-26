package com.dev.lib.entity.id;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class SnowflakeDistributeId {

    private static final long DEFAULT_TWEPOCH = 1542448800000L;
    private static final long WORKER_ID_BITS = 4L;
    private static final long DATACENTER_ID_BITS = 4L;
    private static final long SEQUENCE_BITS = 12L;
    private static final long MAX_WORKER_ID = (1L << WORKER_ID_BITS) - 1;
    private static final long MAX_DATACENTER_ID = (1L << DATACENTER_ID_BITS) - 1;
    private static final long SEQUENCE_MASK = (1L << SEQUENCE_BITS) - 1;
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    private static final long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;
    private static final long CLOCK_BACKWARD_TOLERANCE = 5L;

    private static final Map<String, SnowflakeDistributeId> INSTANCES = new ConcurrentHashMap<>();
    private static final ReentrantLock INSTANCE_LOCK = new ReentrantLock();

    private final long workerId;
    private final long datacenterId;
    private final long twepoch;
    private final ReentrantLock lock = new ReentrantLock();

    private volatile long sequence = 0L;
    private volatile long lastTimestamp = -1L;

    private SnowflakeDistributeId(long workerId, long datacenterId, long twepoch) {
        if (workerId < 0 || workerId > MAX_WORKER_ID) {
            throw new IllegalArgumentException("Worker ID must be between 0 and " + MAX_WORKER_ID);
        }
        if (datacenterId < 0 || datacenterId > MAX_DATACENTER_ID) {
            throw new IllegalArgumentException("Datacenter ID must be between 0 and " + MAX_DATACENTER_ID);
        }
        if (twepoch <= 0 || twepoch >= System.currentTimeMillis()) {
            throw new IllegalArgumentException("Invalid twepoch");
        }

        this.workerId = workerId;
        this.datacenterId = datacenterId;
        this.twepoch = twepoch;

        log.info(
                "SnowflakeDistributeId initialized: workerId={}, datacenterId={}, twepoch={}",
                workerId, datacenterId, twepoch
        );
    }

    public static SnowflakeDistributeId getInstance(long workerId, long datacenterId) {
        return getInstance(workerId, datacenterId, DEFAULT_TWEPOCH);
    }

    public static SnowflakeDistributeId getInstance(long workerId, long datacenterId, long twepoch) {
        String key = workerId + "-" + datacenterId;
        INSTANCE_LOCK.lock();
        try {
            return INSTANCES.computeIfAbsent(key, k -> new SnowflakeDistributeId(workerId, datacenterId, twepoch));
        } finally {
            INSTANCE_LOCK.unlock();
        }
    }

    public long nextId() {
        lock.lock();
        try {
            long timestamp = currentTimeMillis();

            if (timestamp < lastTimestamp) {
                long offset = lastTimestamp - timestamp;
                if (offset <= CLOCK_BACKWARD_TOLERANCE) {
                    log.warn("Clock moved backwards by {} ms, waiting...", offset);
                    timestamp = waitUntilNextMillis(lastTimestamp);
                } else {
                    log.error("Clock moved backwards by {} ms, refusing to generate ID", offset);
                    throw new RuntimeException("Clock moved backwards. Refusing to generate id for " + offset + " milliseconds");
                }
            }

            if (lastTimestamp == timestamp) {
                sequence = (sequence + 1) & SEQUENCE_MASK;
                if (sequence == 0L) {
                    timestamp = waitUntilNextMillis(lastTimestamp);
                }
            } else {
                sequence = 0L;
            }

            lastTimestamp = timestamp;

            return ((timestamp - twepoch) << TIMESTAMP_LEFT_SHIFT)
                    | (datacenterId << DATACENTER_ID_SHIFT)
                    | (workerId << WORKER_ID_SHIFT)
                    | sequence;
        } finally {
            lock.unlock();
        }
    }

    private long waitUntilNextMillis(long lastTimestamp) {
        long timestamp = currentTimeMillis();
        int waitCount = 0;

        while (timestamp <= lastTimestamp) {
            if (waitCount++ < 10) {
                timestamp = currentTimeMillis();
            } else {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while waiting for next millis", e);
                }
                timestamp = currentTimeMillis();
            }
        }

        return timestamp;
    }

    protected long currentTimeMillis() {
        return System.currentTimeMillis();
    }
}