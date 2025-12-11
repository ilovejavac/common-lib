package com.dev.lib.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TreeBuilder {

    /**
     * 通用树构建器
     *
     * @param flatList       扁平数据源
     * @param idMapper       获取ID的方法
     * @param parentIdMapper 获取父ID的方法
     * @param setChildren    设置子节点的方法
     * @param rootValidator  判断是否为根节点的逻辑
     */
    public static <T, ID> List<T> build(
            List<T> flatList,
            Function<T, ID> idMapper,
            Function<T, ID> parentIdMapper,
            BiConsumer<T, List<T>> setChildren,
            Function<T, Boolean> rootValidator
    ) {
        // 1. 映射 ID -> Node
        Map<ID, T> nodeMap = flatList.stream()
                .collect(Collectors.toMap(
                        idMapper,
                        Function.identity()
                ));

        List<T> roots = new ArrayList<>();

        for (T node : flatList) {
            if (rootValidator.apply(node)) {
                roots.add(node);
            } else {
                ID parentId = parentIdMapper.apply(node);
                T  parent   = nodeMap.get(parentId);
                if (parent != null) {
                    // 这里假设 T 类中有 getChildren 方法，或者需要通过反射/回调获取 list
                    // 为简化，这里依赖调用方传入的 setChildren 逻辑，通常需要 T 内部初始化了 List
                    // 实际生产中通常会先包装一层 TreeNode<T>
                    // 此处简化逻辑：调用方负责将 node 添加到 parent 的子列表中
                    setChildren.accept(
                            parent,
                            List.of(node)
                    );
                }
            }
        }
        return roots;
    }

}
