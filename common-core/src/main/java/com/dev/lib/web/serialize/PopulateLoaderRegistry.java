package com.dev.lib.web.serialize;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loader 注册中心，管理所有 PopulateLoader 实例
 */
@Component
@Slf4j
public class PopulateLoaderRegistry implements ApplicationContextAware {
    
    private static final Map<String, PopulateLoader<?, ?>> LOADERS = new ConcurrentHashMap<>();
    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        applicationContext = ctx;
        // 自动注册所有 PopulateLoader Bean
        Map<String, PopulateLoader> beans = ctx.getBeansOfType(PopulateLoader.class);
        beans.forEach((name, loader) -> {
            LOADERS.put(name, loader);
            log.info("Registered PopulateLoader: {}", name);
        });
    }

    /**
     * 获取 loader
     */
    @SuppressWarnings("unchecked")
    public static <K, V> PopulateLoader<K, V> getLoader(String name) {
        PopulateLoader<?, ?> loader = LOADERS.get(name);
        if (loader == null) {
            // 尝试从容器获取（支持懒加载的 Bean）
            try {
                loader = applicationContext.getBean(name, PopulateLoader.class);
                LOADERS.put(name, loader);
            } catch (BeansException e) {
                log.warn("PopulateLoader not found: {}", name);
                return null;
            }
        }
        return (PopulateLoader<K, V>) loader;
    }

    /**
     * 获取所有已注册的 loader 名称
     */
    public static java.util.Set<String> getLoaderNames() {
        return LOADERS.keySet();
    }
}