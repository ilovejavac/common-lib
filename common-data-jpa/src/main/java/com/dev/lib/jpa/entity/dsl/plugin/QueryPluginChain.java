package com.dev.lib.jpa.entity.dsl.plugin;

import com.dev.lib.jpa.entity.JpaEntity;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.PathBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 插件链
 * <p>
 * 使用 SPI 自动发现插件，无需 Spring 容器
 */
@Slf4j
public class QueryPluginChain {

    private static final QueryPluginChain INSTANCE = new QueryPluginChain();

    private final List<QueryPlugin> plugins = new ArrayList<>();

    private final Map<Class<? extends JpaEntity>, List<QueryPlugin>> supportsCache = new ConcurrentHashMap<>(128);

    private QueryPluginChain() {
        // SPI 加载
        ServiceLoader.load(QueryPlugin.class).forEach(plugins::add);
        plugins.sort(Comparator.comparingInt(QueryPlugin::getOrder));
    }

    public static QueryPluginChain getInstance() {

        return INSTANCE;
    }

    /**
     * 手动注册（用于 Spring 环境补充注册）
     */
    public void register(QueryPlugin plugin) {

        plugins.add(plugin);
        plugins.sort(Comparator.comparingInt(QueryPlugin::getOrder));
        supportsCache.clear();
    }

    /**
     * 执行插件链，返回合并后的条件
     */
    public BooleanExpression apply(PathBuilder<?> path, Class<? extends JpaEntity> entityClass) {

        List<QueryPlugin> matched = supportsCache.computeIfAbsent(entityClass, clazz -> {
            List<QueryPlugin> result = new ArrayList<>();
            for (QueryPlugin plugin : plugins) {
                if (plugin.supports(clazz)) {
                    result.add(plugin);
                }
            }
            return result;
        });

        if (matched.isEmpty()) {
            return null;
        }

        BooleanExpression result = null;
        for (QueryPlugin plugin : matched) {
            BooleanExpression expr = plugin.apply(path, entityClass);
            if (expr != null) {
                result = result == null ? expr : result.and(expr);
            }
        }
        return result;
    }

}
