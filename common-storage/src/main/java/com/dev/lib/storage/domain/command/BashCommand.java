package com.dev.lib.storage.domain.command;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Bash 命令抽象基类
 * 负责命令参数解析，子类负责具体执行
 */
public abstract class BashCommand {

    /**
     * 执行命令
     * @param args 动态参数（可以是 String, Integer, Boolean, InputStream 等）
     * @return 命令执行结果
     */
    public abstract Object execute(Object... args);

    /**
     * 解析参数为 Map
     * 支持格式：
     * - 标志："-r", "-f", "-n" -> {r: true, f: true, n: true}
     * - 键值对："-n", "10" -> {n: 10}
     * - 位置参数：保存到 "args" 列表中
     */
    protected ParsedArgs parseArgs(Object... args) {
        ParsedArgs parsed = new ParsedArgs();

        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];

            if (arg instanceof String) {
                String str = (String) arg;

                // 处理标志参数（如 -r, -f, -rf）
                if (str.startsWith("-") && !str.equals("-")) {
                    String flags = str.substring(1);

                    // 处理组合标志（如 -rf）
                    for (char flag : flags.toCharArray()) {
                        // 检查下一个参数是否是值
                        if (i + 1 < args.length && !(args[i + 1] instanceof String && ((String) args[i + 1]).startsWith("-"))) {
                            // 可能是键值对，但需要判断
                            if (flags.length() == 1) {
                                // 单个标志，下一个可能是值
                                Object nextArg = args[i + 1];
                                if (nextArg instanceof Integer || nextArg instanceof Long) {
                                    parsed.options.put(String.valueOf(flag), nextArg);
                                    i++; // 跳过下一个参数
                                    break;
                                } else if (nextArg instanceof String) {
                                    String nextStr = (String) nextArg;
                                    // 尝试解析为数字
                                    try {
                                        int value = Integer.parseInt(nextStr);
                                        parsed.options.put(String.valueOf(flag), value);
                                        i++; // 跳过下一个参数
                                        break;
                                    } catch (NumberFormatException e) {
                                        // 不是数字，当作布尔标志
                                        parsed.options.put(String.valueOf(flag), true);
                                    }
                                } else {
                                    parsed.options.put(String.valueOf(flag), true);
                                }
                            } else {
                                // 组合标志，都当作布尔值
                                parsed.options.put(String.valueOf(flag), true);
                            }
                        } else {
                            // 布尔标志
                            parsed.options.put(String.valueOf(flag), true);
                        }
                    }
                } else {
                    // 位置参数
                    parsed.positionalArgs.add(str);
                }
            } else {
                // 非字符串参数（如 InputStream）
                parsed.positionalArgs.add(arg);
            }
        }

        return parsed;
    }

    /**
     * 解析后的参数
     */
    protected static class ParsedArgs {
        // 选项参数（如 -r -> true, -n -> 10）
        public Map<String, Object> options = new HashMap<>();

        // 位置参数（如文件路径）
        public java.util.List<Object> positionalArgs = new java.util.ArrayList<>();

        public boolean hasFlag(String flag) {
            return Boolean.TRUE.equals(options.get(flag));
        }

        public Integer getInt(String key, Integer defaultValue) {
            Object value = options.get(key);
            if (value instanceof Integer) {
                return (Integer) value;
            }
            return defaultValue;
        }

        public String getString(int index) {
            if (index < positionalArgs.size() && positionalArgs.get(index) instanceof String) {
                return (String) positionalArgs.get(index);
            }
            return null;
        }

        public <T> T get(int index, Class<T> type) {
            if (index < positionalArgs.size() && type.isInstance(positionalArgs.get(index))) {
                return type.cast(positionalArgs.get(index));
            }
            return null;
        }

        public int positionalCount() {
            return positionalArgs.size();
        }
    }
}
