package com.dev.lib.cache.common;

import org.redisson.api.RAtomicLong;

import java.time.Duration;

public class CacheAtomicLong {
    private final String key;
    private final Duration ttl;
    private final RAtomicLong rAtomicLong;

    public CacheAtomicLong(String key, Duration ttl, RAtomicLong rAtomicLong) {
        this.key = key;
        this.ttl = ttl;
        this.rAtomicLong = rAtomicLong;
    }

    private void expireIfNeeded() {
        if (ttl != null && rAtomicLong.isExists()) {
            rAtomicLong.expire(ttl);
        }
    }

    public long get() {
        return rAtomicLong.get();
    }

    public void set(long newValue) {
        rAtomicLong.set(newValue);
        expireIfNeeded();
    }

    public long getAndSet(long newValue) {
        long result = rAtomicLong.getAndSet(newValue);
        expireIfNeeded();
        return result;
    }

    public long incrementAndGet() {
        long result = rAtomicLong.incrementAndGet();
        expireIfNeeded();
        return result;
    }

    public long getAndIncrement() {
        long result = rAtomicLong.getAndIncrement();
        expireIfNeeded();
        return result;
    }

    public long decrementAndGet() {
        long result = rAtomicLong.decrementAndGet();
        expireIfNeeded();
        return result;
    }

    public long getAndDecrement() {
        long result = rAtomicLong.getAndDecrement();
        expireIfNeeded();
        return result;
    }

    public long addAndGet(long delta) {
        long result = rAtomicLong.addAndGet(delta);
        expireIfNeeded();
        return result;
    }

    public long getAndAdd(long delta) {
        long result = rAtomicLong.getAndAdd(delta);
        expireIfNeeded();
        return result;
    }

    public boolean compareAndSet(long expect, long update) {
        boolean result = rAtomicLong.compareAndSet(expect, update);
        if (result) {
            expireIfNeeded();
        }
        return result;
    }

    public RAtomicLong raw() {
        return rAtomicLong;
    }
}