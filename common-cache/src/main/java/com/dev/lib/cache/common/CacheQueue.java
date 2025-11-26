package com.dev.lib.cache.common;

import org.redisson.api.RQueue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class CacheQueue<T> {
    private final String key;
    private final Duration ttl;
    private final RQueue<T> rQueue;

    public CacheQueue(String key, Duration ttl, RQueue<T> rQueue) {
        this.key = key;
        this.ttl = ttl;
        this.rQueue = rQueue;
    }

    private void expireIfNeeded() {
        if (ttl != null && rQueue.isExists()) {
            rQueue.expire(ttl);
        }
    }

    public boolean isEmpty() {
        return rQueue.isEmpty();
    }

    public int size() {
        return rQueue.size();
    }

    public boolean offer(T element) {
        boolean result = rQueue.offer(element);
        if (result) {
            expireIfNeeded();
        }
        return result;
    }

    public boolean add(T e) {
        boolean result = rQueue.add(e);
        if (result) {
            expireIfNeeded();
        }
        return result;
    }

    public T poll() {
        T result = rQueue.poll();
        expireIfNeeded();
        return result;
    }

    public T remove() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        T result = rQueue.remove();
        expireIfNeeded();
        return result;
    }

    public T peek() {
        return rQueue.peek();
    }

    public T element() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        return rQueue.element();
    }

    public void clear() {
        rQueue.clear();
    }

    public List<T> readAll() {
        return new ArrayList<>(rQueue.readAll());
    }

    public boolean contains(Object o) {
        return rQueue.contains(o);
    }

    public RQueue<T> raw() {
        return rQueue;
    }
}