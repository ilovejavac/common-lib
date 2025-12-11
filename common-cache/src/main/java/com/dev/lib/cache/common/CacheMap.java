package com.dev.lib.cache.common;

import org.redisson.api.RMap;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

@SuppressWarnings("unchecked")
public class CacheMap {

    private final String               key;

    private final Duration             ttl;

    private final RMap<Object, Object> rMap;

    public CacheMap(String key, Duration ttl, RMap<Object, Object> rMap) {

        this.key = key;
        this.ttl = ttl;
        this.rMap = rMap;
    }

    private void expireIfNeeded() {

        if (ttl != null && rMap.isExists()) {
            rMap.expire(ttl);
        }
    }

    public <K, V> RMap<K, V> raw() {

        return (RMap<K, V>) rMap;
    }

    public <V> V get(Object field) {

        return (V) rMap.get(field);
    }

    public <V> V getOrDefault(Object key, V defaultValue) {

        return (V) rMap.getOrDefault(
                key,
                defaultValue
        );
    }

    public <V> void put(Object field, V value) {

        rMap.put(
                field,
                value
        );
        expireIfNeeded();
    }

    public <V> V putIfAbsent(Object key, V value) {

        V result = (V) rMap.putIfAbsent(
                key,
                value
        );
        if (result == null) {
            expireIfNeeded();
        }
        return result;
    }

    public <V> void putAll(Map<?, V> map) {

        rMap.putAll(map);
        expireIfNeeded();
    }

    public <K, V> V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {

        V result = (V) rMap.computeIfAbsent(
                key,
                k -> mappingFunction.apply((K) k)
        );
        expireIfNeeded();
        return result;
    }

    public <K, V> V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {

        V result = (V) rMap.computeIfPresent(
                key,
                (k, v) -> remappingFunction.apply(
                        (K) k,
                        (V) v
                )
        );
        expireIfNeeded();
        return result;
    }

    public <K, V> V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {

        V result = (V) rMap.compute(
                key,
                (k, v) -> remappingFunction.apply(
                        (K) k,
                        (V) v
                )
        );
        expireIfNeeded();
        return result;
    }

    public boolean remove(Object field) {

        boolean removed = rMap.remove(field) != null;
        if (removed) {
            expireIfNeeded();
        }
        return removed;
    }

    public boolean containsKey(Object field) {

        return rMap.containsKey(field);
    }

    public boolean containsValue(Object value) {

        return rMap.containsValue(value);
    }

    public void clear() {

        rMap.clear();
    }

    public int size() {

        return rMap.size();
    }

    public boolean isEmpty() {

        return rMap.isEmpty();
    }

    public Set<Object> keySet() {

        return rMap.keySet();
    }

    public Collection<Object> values() {

        return rMap.values();
    }

    public Set<Map.Entry<Object, Object>> entrySet() {

        return rMap.entrySet();
    }

    public Map<Object, Object> readAll() {

        return new HashMap<>(rMap);
    }

}