package com.dev.lib.entity.id;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

/**
 * 独立读取 YAML 配置，完全不依赖 Spring，供静态初始化使用。
 * 支持多环境：application.yaml + application-{profile}.yaml 合并覆盖。
 */
class SnowflakeConfigLoader {

    private final long workerId;

    private static final SnowflakeConfigLoader INSTANCE;

    static {
        INSTANCE = load();
    }

    private SnowflakeConfigLoader(long workerId) {
        this.workerId = workerId;
    }

    static SnowflakeConfigLoader getInstance() {
        return INSTANCE;
    }

    long getWorkerId() {
        return workerId;
    }

    // -------------------------------------------------------------------------

    private static SnowflakeConfigLoader load() {
        // 1. 加载基础配置
        Map<String, Object> base = loadYaml("application.yaml");
        if (base == null) base = new java.util.LinkedHashMap<>();

        // 2. 检测激活的 profile（优先级：JVM 属性 > yaml 配置）
        String profile = System.getProperty("spring.profiles.active");
        if (isBlank(profile)) profile = System.getProperty("app.profile");
        if (isBlank(profile)) profile = getNestedString(base, "spring", "profiles", "active");
        if (isBlank(profile)) profile = getNestedString(base, "app", "profile");

        // 3. 加载并合并 profile 配置（覆盖基础值）
        if (!isBlank(profile)) {
            Map<String, Object> profileConfig = loadYaml("application-" + profile + ".yaml");
            if (profileConfig != null) {
                deepMerge(base, profileConfig);
            }
        }

        // 4. 解析 app.snow-flake.worker-id，clamp 到 [0, 15]
        long workerId = clamp(getNestedInt(base, "app", "snow-flake", "worker-id"), 0, 15);

        return new SnowflakeConfigLoader(workerId);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> loadYaml(String filename) {
        try (InputStream in = SnowflakeConfigLoader.class.getClassLoader().getResourceAsStream(filename)) {
            if (in == null) return null;
            return new Yaml().load(in);
        } catch (Exception e) {
            throw new ExceptionInInitializerError("读取配置文件失败: " + filename + " -> " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private static void deepMerge(Map<String, Object> target, Map<String, Object> source) {
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            Object srcVal = entry.getValue();
            Object tgtVal = target.get(entry.getKey());
            if (srcVal instanceof Map && tgtVal instanceof Map) {
                deepMerge((Map<String, Object>) tgtVal, (Map<String, Object>) srcVal);
            } else {
                target.put(entry.getKey(), srcVal);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static String getNestedString(Map<String, Object> map, String... keys) {
        Object cur = map;
        for (String key : keys) {
            if (!(cur instanceof Map)) return null;
            cur = ((Map<String, Object>) cur).get(key);
        }
        return cur instanceof String ? (String) cur : null;
    }

    @SuppressWarnings("unchecked")
    private static int getNestedInt(Map<String, Object> map, String... keys) {
        Object cur = map;
        for (String key : keys) {
            if (!(cur instanceof Map)) return 0;
            cur = ((Map<String, Object>) cur).get(key);
        }
        return cur instanceof Number ? ((Number) cur).intValue() : 0;
    }

    private static long clamp(long val, long min, long max) {
        return Math.max(min, Math.min(max, val));
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
