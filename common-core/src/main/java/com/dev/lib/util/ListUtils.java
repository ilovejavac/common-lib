package com.dev.lib.util;

import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.factory.Lists;

import java.security.SecureRandom;
import java.util.List;
import java.util.function.Predicate;

public abstract class ListUtils {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    static {
        SECURE_RANDOM.nextInt();
    }

    /**
     * 从列表随机获取一个元素
     */
    public static <T> T randomGet(List<T> list) {

        if (list == null || list.isEmpty()) {
            throw new IllegalArgumentException("List cannot be null or empty");
        }
        int index = SECURE_RANDOM.nextInt(list.size());
        return list.get(index);
    }

    /**
     * 从ImmutableList随机获取一个元素
     */
    public static <T> T randomGet(ImmutableList<T> list) {

        if (list == null || list.isEmpty()) {
            throw new IllegalArgumentException("List cannot be null or empty");
        }
        int index = SECURE_RANDOM.nextInt(list.size());
        return list.get(index);
    }

    /**
     * 安全获取元素（返回null而不抛异常）
     */
    public static <T> T randomGetOrNull(List<T> list) {

        if (list == null || list.isEmpty()) {
            return null;
        }
        int index = SECURE_RANDOM.nextInt(list.size());
        return list.get(index);
    }

    /**
     * 随机获取N个不重复元素
     */
    public static <T> ImmutableList<T> randomGetN(List<T> list, int n) {

        if (list == null || list.isEmpty()) {
            return Lists.immutable.empty();
        }
        if (n >= list.size()) {
            return Lists.immutable.ofAll(list);
        }

        return Lists.immutable.ofAll(list)
                .toList()
                .shuffleThis(SECURE_RANDOM)
                .take(n)
                .toImmutable();
    }

    /**
     * 过滤后随机获取
     */
    public static <T> T randomGetFiltered(List<T> list, Predicate<T> filter) {

        if (list == null || list.isEmpty()) {
            throw new IllegalArgumentException("List cannot be null or empty");
        }

        ImmutableList<T> filtered = Lists.immutable.ofAll(list).select(filter::test);
        if (filtered.isEmpty()) {
            throw new IllegalArgumentException("No elements match the filter");
        }

        return randomGet(filtered);
    }

    /**
     * 判空
     */
    public static boolean isEmpty(List<?> list) {

        return list == null || list.isEmpty();
    }

    public static boolean isEmpty(ImmutableList<?> list) {

        return list == null || list.isEmpty();
    }

    /**
     * 判非空
     */
    public static boolean isNotEmpty(List<?> list) {

        return !isEmpty(list);
    }

    public static boolean isNotEmpty(ImmutableList<?> list) {

        return !isEmpty(list);
    }

}
