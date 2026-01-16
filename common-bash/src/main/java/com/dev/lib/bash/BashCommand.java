package com.dev.lib.bash;

import java.util.*;

/**
 * Bash 命令抽象基类
 * 负责命令参数解析，子类负责具体执行
 */
public abstract class BashCommand<T extends Object> {

    /**
     * 执行命令
     * @param ctx 执行上下文
     * @return 执行结果
     */
    public abstract T execute(ExecuteContext ctx);

    /**
     * 解析命令行字符串为 token 数组
     * 支持：
     * - 空格分隔：ls -la /path
     * - 单引号：cat 'file with spaces.txt'
     * - 双引号：echo "hello world"
     * - 转义：cat file\ with\ spaces.txt
     *
     * @param commandLine 命令行字符串
     * @return 解析后的 token 数组
     */
    protected String[] parseCommandLine(String commandLine) {

        if (commandLine == null || commandLine.trim().isEmpty()) {
            return new String[0];
        }

        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean escaped = false;

        for (int i = 0; i < commandLine.length(); i++) {
            char c = commandLine.charAt(i);

            if (escaped) {
                current.append(c);
                escaped = false;
                continue;
            }

            if (c == '\\' && !inSingleQuote) {
                escaped = true;
                continue;
            }

            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
                continue;
            }

            if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
                continue;
            }

            if (c == ' ' && !inSingleQuote && !inDoubleQuote) {
                if (current.length() > 0) {
                    tokens.add(current.toString());
                    current = new StringBuilder();
                }
                continue;
            }

            current.append(c);
        }

        if (current.length() > 0) {
            tokens.add(current.toString());
        }

        return tokens.toArray(new String[0]);
    }

    /**
     * 解析参数为 Map
     * 支持格式：
     * - 标志："-r", "-f", "-n" -> {r: true, f: true, n: true}
     * - 键值对："-n", "10" -> {n: 10}
     * - 组合标志+值："-rn 10" -> {r: true, n: 10}（最后一个标志可以带值）
     * - 位置参数：保存到 "args" 列表中
     */
    protected ParsedArgs parseArgs(String... args) {

        ParsedArgs parsed = new ParsedArgs();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            if (arg == null) continue;

            // 处理标志参数（如 -r, -f, -rf）
            if (arg.startsWith("-") && !arg.equals("-")) {
                // 处理长选项（如 --depth 3）
                if (arg.startsWith("--") && arg.length() > 2) {
                    String longFlag = arg.substring(2);
                    // 检查下一个参数是否是值
                    if (i + 1 < args.length && !isOptionLike(args[i + 1])) {
                        String nextArg = args[i + 1];
                        Object value = tryParseValue(nextArg);
                        if (value != null) {
                            parsed.options.put(longFlag, value);
                            i++; // 跳过值参数
                            continue;
                        }
                    }
                    // 长选项作为布尔值
                    parsed.options.put(longFlag, true);
                    continue;
                }

                // 处理短选项（如 -r, -f, -rf）
                String flags = arg.substring(1);

                // 检查下一个参数是否是值（仅当最后一个标志是已知的"带值标志"时）
                char lastFlag = flags.charAt(flags.length() - 1);
                // -n: 带数值的标志（如 head/tail -n 10）
                // 其他标志都是布尔值
                boolean canTakeValue = (lastFlag == 'n');

                if (canTakeValue && i + 1 < args.length && !isOptionLike(args[i + 1])) {
                    String nextArg = args[i + 1];
                    Object value = tryParseValue(nextArg);

                    if (value != null) {
                        // 下一个参数是值，绑定到组合标志的最后一个字符
                        // 例如：-rn 10 → {r: true, n: 10}
                        parsed.options.put(String.valueOf(lastFlag), value);
                        i++; // 跳过值参数

                        // 处理前面的标志（都作为布尔值）
                        for (int j = 0; j < flags.length() - 1; j++) {
                            parsed.options.put(String.valueOf(flags.charAt(j)), true);
                        }
                        continue;
                    }
                }

                // 所有标志都作为布尔值
                for (char flag : flags.toCharArray()) {
                    parsed.options.put(String.valueOf(flag), true);
                }
            } else {
                // 位置参数
                parsed.positionalArgs.add(arg);
            }
        }

        return parsed;
    }

    /**
     * 检查参数是否类似选项（以 - 开头）
     */
    private boolean isOptionLike(String arg) {
        return arg != null && arg.startsWith("-");
    }

    /**
     * 尝试将参数解析为值（数字）
     * 返回 null 表示不是值类型
     */
    private Object tryParseValue(String arg) {

        if (arg == null) return null;

        // 尝试解析为数字
        try {
            return Integer.parseInt(arg);
        } catch (NumberFormatException e) {
            // 不是数字
            // 但如果是以 - 开头，则可能是选项，返回 null
            if (arg.startsWith("-")) {
                return null;
            }
            return arg;
        }
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
