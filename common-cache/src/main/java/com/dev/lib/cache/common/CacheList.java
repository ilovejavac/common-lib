package com.dev.lib.cache.common;

import org.redisson.api.RList;

import java.time.Duration;
import java.util.Collection;
import java.util.List;

public class CacheList<T> {

    private final String key;

    private final Duration ttl;

    private final RList<T> rList;

    public CacheList(String key, Duration ttl, RList<T> rList) {

        this.key = key;
        this.ttl = ttl;
        this.rList = rList;
    }

    private void expireIfNeeded() {

        if (ttl != null && rList.isExists()) {
            rList.expire(ttl);
        }
    }

    public boolean isEmpty() {

        return rList.isEmpty();
    }

    public int size() {

        return rList.size();
    }

    public boolean add(T element) {

        boolean result = rList.add(element);
        if (result) {
            expireIfNeeded();
        }
        return result;
    }

    public boolean addAll(Collection<? extends T> c) {

        boolean result = rList.addAll(c);
        if (result) {
            expireIfNeeded();
        }
        return result;
    }

    public boolean addAll(int index, Collection<? extends T> c) {

        boolean result = rList.addAll(
                index,
                c
        );
        if (result) {
            expireIfNeeded();
        }
        return result;
    }

    public T get(int index) {

        return rList.get(index);
    }

    public void set(int index, T element) {

        rList.set(
                index,
                element
        );
        expireIfNeeded();
    }

    public boolean remove(Object o) {

        boolean result = rList.remove(o);
        if (result) {
            expireIfNeeded();
        }
        return result;
    }

    public T remove(int index) {

        T result = rList.remove(index);
        expireIfNeeded();
        return result;
    }

    public void clear() {

        rList.clear();
    }

    public List<T> readAll() {

        return rList.readAll();
    }

    public boolean contains(Object o) {

        return rList.contains(o);
    }

    public List<T> subList(int fromIndex, int toIndex) {

        return rList.subList(
                fromIndex,
                toIndex
        );
    }

    public int indexOf(Object o) {

        return rList.indexOf(o);
    }

    public int lastIndexOf(Object o) {

        return rList.lastIndexOf(o);
    }

    public RList<T> raw() {

        return rList;
    }

}