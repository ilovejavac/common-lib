package com.dev.lib.bash;

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
     * - 组合标志+值："-rn 10" -> {r: true, n: 10}（最后一个标志可以带值）
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

                    // 检查下一个参数是否是值
                    if (i + 1 < args.length && !isOptionLike(args[i + 1])) {
                        // 下一个参数可能是值，尝试解析
                        Object nextArg = args[i + 1];
                        Object value = tryParseValue(nextArg);

                        if (value != null) {
                            // 下一个参数是值，绑定到组合标志的最后一个字符
                            // 例如：-rn 10 → {r: true, n: 10}
                            char lastFlag = flags.charAt(flags.length() - 1);
                            parsed.options.put(String.valueOf(lastFlag), value);
                            i++; // 跳过值参数

                            // 处理前面的标志（都作为布尔值）
                            for (int j = 0; j < flags.length() - 1; j++) {
                                parsed.options.put(String.valueOf(flags.charAt(j)), true);
                            }
                        } else {
                            // 下一个参数不是值，所有标志都作为布尔值
                            for (char flag : flags.toCharArray()) {
                                parsed.options.put(String.valueOf(flag), true);
                            }
                        }
                    } else {
                        // 下一个参数是选项或没有下一个参数，所有标志都作为布尔值
                        for (char flag : flags.toCharArray()) {
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
     * 检查参数是否类似选项（以 - 开头）
     */
    private boolean isOptionLike(Object arg) {
        if (arg instanceof String) {
            String str = (String) arg;
            return str.startsWith("-");
        }
        return false;
    }

    /**
     * 尝试将参数解析为值（数字或字符串）
     * 返回 null 表示不是值类型
     */
    private Object tryParseValue(Object arg) {
        if (arg instanceof Integer || arg instanceof Long) {
            return arg;
        }
        if (arg instanceof String) {
            String str = (String) arg;
            // 尝试解析为数字
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException e) {
                // 不是数字，返回字符串作为值
                // 但如果是以 - 开头，则可能是选项，返回 null
                if (str.startsWith("-")) {
                    return null;
                }
                return str;
            }
        }
        // 其他类型不作为值处理
        return null;
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
