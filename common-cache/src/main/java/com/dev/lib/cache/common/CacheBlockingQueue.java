package com.dev.lib.cache.common;

import org.redisson.api.RBlockingQueue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CacheBlockingQueue<T> {

    private final String key;

    private final Duration ttl;

    private final RBlockingQueue<T> rBlockingQueue;

    public CacheBlockingQueue(String key, Duration ttl, RBlockingQueue<T> rBlockingQueue) {

        this.key = key;
        this.ttl = ttl;
        this.rBlockingQueue = rBlockingQueue;
    }

    private void expireIfNeeded() {

        if (ttl != null && rBlockingQueue.isExists()) {
            rBlockingQueue.expire(ttl);
        }
    }

    public void put(T e) throws InterruptedException {

        rBlockingQueue.put(e);
        expireIfNeeded();
    }

    public boolean offer(T e, long timeout, TimeUnit unit) throws InterruptedException {

        boolean result = rBlockingQueue.offer(
                e,
                timeout,
                unit
        );
        if (result) {
            expireIfNeeded();
        }
        return result;
    }

    public T take() throws InterruptedException {

        T result = rBlockingQueue.take();
        expireIfNeeded();
        return result;
    }

    public T poll(long timeout, TimeUnit unit) throws InterruptedException {

        T result = rBlockingQueue.poll(
                timeout,
                unit
        );
        expireIfNeeded();
        return result;
    }

    public boolean offer(T e) {

        boolean result = rBlockingQueue.offer(e);
        if (result) {
            expireIfNeeded();
        }
        return result;
    }

    public T poll() {

        T result = rBlockingQueue.poll();
        expireIfNeeded();
        return result;
    }

    public T peek() {

        return rBlockingQueue.peek();
    }

    public int drainTo(Collection<? super T> c) {

        int result = rBlockingQueue.drainTo(c);
        if (result > 0) {
            expireIfNeeded();
        }
        return result;
    }

    public int drainTo(Collection<? super T> c, int maxElements) {

        int result = rBlockingQueue.drainTo(
                c,
                maxElements
        );
        if (result > 0) {
            expireIfNeeded();
        }
        return result;
    }

    public boolean isEmpty() {

        return rBlockingQueue.isEmpty();
    }

    public int size() {

        return rBlockingQueue.size();
    }

    public void clear() {

        rBlockingQueue.clear();
    }

    public List<T> readAll() {

        return new ArrayList<>(rBlockingQueue.readAll());
    }

    public RBlockingQueue<T> raw() {

        return rBlockingQueue;
    }

}