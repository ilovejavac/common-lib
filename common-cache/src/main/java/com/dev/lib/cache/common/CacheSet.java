package com.dev.lib.cache.common;

import org.redisson.api.RSet;

import java.time.Duration;
import java.util.Collection;
import java.util.Set;

public class CacheSet<T> {

    private final String   key;

    private final Duration ttl;

    private final RSet<T>  rSet;

    public CacheSet(String key, Duration ttl, RSet<T> rSet) {

        this.key = key;
        this.ttl = ttl;
        this.rSet = rSet;
    }

    private void expireIfNeeded() {

        if (ttl != null && rSet.isExists()) {
            rSet.expire(ttl);
        }
    }

    public boolean isEmpty() {

        return rSet.isEmpty();
    }

    public int size() {

        return rSet.size();
    }

    public boolean add(T element) {

        boolean result = rSet.add(element);
        if (result) {
            expireIfNeeded();
        }
        return result;
    }

    public boolean addAll(Collection<? extends T> c) {

        boolean result = rSet.addAll(c);
        if (result) {
            expireIfNeeded();
        }
        return result;
    }

    public boolean remove(Object o) {

        boolean result = rSet.remove(o);
        if (result) {
            expireIfNeeded();
        }
        return result;
    }

    public boolean removeAll(Collection<?> c) {

        boolean result = rSet.removeAll(c);
        if (result) {
            expireIfNeeded();
        }
        return result;
    }

    public boolean retainAll(Collection<?> c) {

        boolean result = rSet.retainAll(c);
        if (result) {
            expireIfNeeded();
        }
        return result;
    }

    public void clear() {

        rSet.clear();
    }

    public Set<T> readAll() {

        return rSet.readAll();
    }

    public boolean contains(Object o) {

        return rSet.contains(o);
    }

    public boolean containsAll(Collection<?> c) {

        return rSet.containsAll(c);
    }

    public RSet<T> raw() {

        return rSet;
    }

}