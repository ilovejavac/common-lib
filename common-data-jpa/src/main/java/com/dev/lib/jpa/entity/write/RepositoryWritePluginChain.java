package com.dev.lib.jpa.entity.write;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
public class RepositoryWritePluginChain {

    private static final RepositoryWritePluginChain INSTANCE = new RepositoryWritePluginChain();

    private final CopyOnWriteArrayList<RepositoryWritePlugin> plugins = new CopyOnWriteArrayList<>();

    private RepositoryWritePluginChain() {

        ServiceLoader.load(RepositoryWritePlugin.class).forEach(this::register);
    }

    public static RepositoryWritePluginChain getInstance() {

        return INSTANCE;
    }

    public Registration register(RepositoryWritePlugin plugin) {

        if (plugin == null) {
            return () -> {
            };
        }
        plugins.addIfAbsent(plugin);
        sortPlugins();
        return () -> plugins.remove(plugin);
    }

    public Optional<RepositoryWritePlugin> resolve(RepositoryWriteContext<?> context) {

        List<RepositoryWritePlugin> matched = new ArrayList<>();
        for (RepositoryWritePlugin plugin : plugins) {
            if (plugin.supports(context)) {
                matched.add(plugin);
            }
        }

        if (matched.isEmpty()) {
            return Optional.empty();
        }
        matched.sort(Comparator.comparingInt(RepositoryWritePlugin::getOrder));
        int winningOrder = matched.getFirst().getOrder();
        List<RepositoryWritePlugin> winners = matched.stream()
                .filter(plugin -> plugin.getOrder() == winningOrder)
                .toList();
        if (winners.size() > 1) {
            throw new IllegalStateException("多个同优先级 RepositoryWritePlugin 同时匹配实体 "
                    + context.entityClass().getName()
                    + "，datasource=" + context.datasourceName()
                    + "，logicalDialect=" + context.logicalDialect()
                    + "，请调整插件 supports() 或 order 配置: "
                    + winners.stream().map(plugin -> plugin.getClass().getName()).toList());
        }
        return Optional.of(winners.getFirst());
    }

    private void sortPlugins() {

        plugins.sort(Comparator.comparingInt(RepositoryWritePlugin::getOrder));
    }

    @FunctionalInterface
    public interface Registration {

        void unregister();
    }
}
