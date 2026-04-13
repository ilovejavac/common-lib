package com.dev.lib.entity.id;

import lombok.extern.slf4j.Slf4j;

import java.util.Random;

@Slf4j
public class SnowflakeDistributeId {

    private static final Random RANDOM = new Random();

    private static final long TWEPOCH = 1542452400000L; // 2018-11-17

    private static final int WORKER_ID_BITS = 4;
    private static final int RAND_BITS      = 4;
    private static final int SEQUENCE_BITS  = 12;

    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS); // 15
    private static final long RAND_MASK     = ~(-1L << RAND_BITS);      // 15
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);  // 4095

    private static volatile SnowflakeDistributeId instance;

    private final long workerId;

    private long rand;
    private long sequence      = 0L;
    private long lastTimestamp = -1L;

    private SnowflakeDistributeId(long workerId) {
        if (workerId < 0 || workerId > MAX_WORKER_ID) {
            throw new IllegalArgumentException("workerId must be between 0 and " + MAX_WORKER_ID);
        }
        this.workerId = workerId;
        this.rand = RANDOM.nextInt() & RAND_MASK;
    }

    public static SnowflakeDistributeId getInstance(long workerId) {
        if (instance != null) return instance;
        synchronized (SnowflakeDistributeId.class) {
            if (instance == null) {
                instance = new SnowflakeDistributeId(workerId & MAX_WORKER_ID);
            }
        }
        return instance;
    }

    public synchronized long nextId() {
        long timestamp = currentTimeMillis();

        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                timestamp = untilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        // 时钟回拨：rand 自增规避，不等待不抛异常
        if (timestamp < lastTimestamp) {
            rand = (rand + 1) & RAND_MASK;
            timestamp = untilNextMillis(lastTimestamp);
            sequence = 0L;
            log.warn("[Snowflake] Clock moved backwards, rand incremented to {}", rand);
        }

        lastTimestamp = timestamp;

        long id = (timestamp - TWEPOCH);
        id = (id << WORKER_ID_BITS) | workerId;
        id = (id << RAND_BITS)      | rand;
        id = (id << SEQUENCE_BITS)  | sequence;
        return id;
    }

    protected long untilNextMillis(long last) {
        long timestamp = currentTimeMillis();
        while (timestamp <= last) {
            timestamp = currentTimeMillis();
        }
        return timestamp;
    }

    protected long currentTimeMillis() {
        return System.currentTimeMillis();
    }
}
