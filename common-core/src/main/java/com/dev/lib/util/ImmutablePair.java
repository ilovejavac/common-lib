package com.dev.lib.util;

import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Objects;

/**
 * 一个不可变的（Immutable）泛型 Pair 类，用于存储两个值。
 * 推荐在需要临时存储两个相关数据时使用。
 *
 * @param <K> 第一个元素的类型
 * @param <V> 第二个元素的类型
 */
@Getter
@Accessors(fluent = true)
public final class ImmutablePair<K, V> {

    private final K key; // 命名为 key 或 left

    private final V value; // 命名为 value 或 right

    /**
     * 构造一个新的 Pair 实例。
     *
     * @param key   第一个元素
     * @param value 第二个元素
     */
    public ImmutablePair(K key, V value) {

        this.key = key;
        this.value = value;
    }

    // 静态工厂方法，简化创建过程
    public static <K, V> ImmutablePair<K, V> of(K key, V value) {

        return new ImmutablePair<>(
                key,
                value
        );
    }

    // --- 标准方法覆盖 ---

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ImmutablePair<?, ?> that = (ImmutablePair<?, ?>) o;
        // 使用 Objects.equals() 安全地比较可能为 null 的字段
        return Objects.equals(
                key,
                that.key
        ) &&
                Objects.equals(
                        value,
                        that.value
                );
    }

    @Override
    public int hashCode() {
        // 使用 Objects.hash() 生成 hash code
        return Objects.hash(
                key,
                value
        );
    }

    @Override
    public String toString() {

        return "ImmutablePair{" +
                "key=" + key +
                ", value=" + value +
                '}';
    }

}