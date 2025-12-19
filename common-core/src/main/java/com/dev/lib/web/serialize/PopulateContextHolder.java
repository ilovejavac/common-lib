package com.dev.lib.web.serialize;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 请求级缓存，按 loader 名称隔离
 */
@Slf4j
public abstract class PopulateContextHolder {

    private PopulateContextHolder() {

    }

    // Map<loaderName, Map<key, value>>
    private static final ThreadLocal<Map<String, Map<Object, Object>>> CACHE =
            ThreadLocal.withInitial(HashMap::new);

    /**
     * 批量预加载
     */
    @SuppressWarnings("unchecked")
// PopulateContextHolder.java
    public static <K, V> void preload(String loaderName, Set<K> keys, PopulateLoader<K, V> loader) {

        if (keys == null || keys.isEmpty()) return;

        Map<Object, Object> loaderCache = CACHE.get().computeIfAbsent(
                loaderName,
                k -> new HashMap<>()
        );

        Set<K> toLoad = keys.stream()
                .filter(key -> key != null && !loaderCache.containsKey(key))
                .collect(java.util.stream.Collectors.toSet());

        if (!toLoad.isEmpty()) {
            Map<K, V> loaded = loader.batchLoad(toLoad);
            if (loaded != null) {
                loaderCache.putAll(loaded);
            }
        }
    }

    /**
     * 获取缓存值
     */
    @SuppressWarnings("unchecked")
    public static <V> V get(String loaderName, Object key) {

        if (key == null) return null;
        Map<Object, Object> loaderCache = CACHE.get().get(loaderName);
        return loaderCache != null ? (V) loaderCache.get(key) : null;
    }

    /**
     * 清理
     */
    public static void clear() {

        CACHE.remove();
    }

}