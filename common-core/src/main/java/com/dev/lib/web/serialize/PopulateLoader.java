package com.dev.lib.web.serialize;

import java.util.Map;
import java.util.Set;

/**
 * 字段填充加载器接口
 *
 * @param <K> 键类型（通常是 Long/String）
 * @param <V> 值类型（填充后的对象）
 */
public interface PopulateLoader<K, V> {

    /**
     * 批量加载
     *
     * @param keys 需要加载的键集合
     * @return 键值映射
     */
    Map<K, V> batchLoad(Set<K> keys);

}