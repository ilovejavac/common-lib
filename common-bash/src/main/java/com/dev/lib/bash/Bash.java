package com.dev.lib.bash;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Bash 命令统一入口，类似 Storage 和 Vfs 的设计
 *
 * 用法：
 * - Bash.exec("ls -la /path")
 * - Bash.root("/bucket").exec("cat file.txt")
 * - Bash.exec("grep -r 'pattern' /path")
 */
@Component
@RequiredArgsConstructor
public class Bash {

    private final BashCommandRegistry registry;

    private static Bash instance;

    @jakarta.annotation.PostConstruct
    public void init() {
        instance = this;
    }

    private static BashCommandRegistry registry() {
        if (instance == null) {
            throw new IllegalStateException("Bash is not initialized");
        }
        return instance.registry;
    }

    // ==================== 静态入口 ====================

    /**
     * 执行 bash 命令
     * @param command 完整命令行，如 "ls -la /path"
     * @return 命令执行结果
     */
    public static Object exec(String command) {
        return exec(command, null);
    }

    /**
     * 执行 bash 命令（指定根路径）
     * @param command 完整命令行
     * @param root 根路径（VFS 场景）
     * @return 命令执行结果
     */
    public static Object exec(String command, String root) {
        if (command == null || command.isBlank()) {
            throw new IllegalArgumentException("command must not be empty");
        }

        String[] tokens = parseCommandLine(command);
        if (tokens.length == 0) {
            throw new IllegalArgumentException("command must not be empty");
        }

        String cmdName = tokens[0];

        // 检测是否包含 shell 特殊字符（管道、重定向、命令链等）
        if (isComplexShellCommand(command)) {
            // 使用 vfsbash 执行复杂命令
            command = "vfsbash -c '" + command.replace("'", "'\\''") + "'";
            tokens = parseCommandLine(command);
            cmdName = tokens[0];
        }

        var registered = registry().getCommands().get(cmdName);

        if (registered == null) {
            throw new IllegalArgumentException("Unknown command: " + cmdName);
        }

        ExecuteContext ctx = new SimpleExecuteContext(root, command);
        return registered.command().execute(ctx);
    }

    /**
     * 检测是否是复杂的 shell 命令（包含管道、重定向、命令链等）
     */
    private static boolean isComplexShellCommand(String command) {
        // 检测 shell 特殊字符
        return command.contains("|") ||   // 管道
               command.contains("&&") ||  // 命令链
               command.contains("||") ||  // 或命令链
               command.contains(";") ||   // 命令分隔
               command.contains(">") ||   // 重定向
               command.contains("<") ||   // 输入重定向
               command.contains("$") ||   // 变量
               command.contains("`") ||   // 命令替换
               command.contains("for ") || // 循环
               command.contains("while ") ||
               command.contains("if ");    // 条件
    }

    /**
     * 链式入口：指定根路径
     */
    public static ContextBuilder root(String root) {
        return new ContextBuilder(root);
    }

    /**
     * 链式构建器
     */
    public static class ContextBuilder {
        private final String root;

        ContextBuilder(String root) {
            this.root = root;
        }

        public Object exec(String command) {
            return Bash.exec(command, root);
        }
    }

    // ==================== 工具方法 ====================

    private static String[] parseCommandLine(String commandLine) {
        if (commandLine == null || commandLine.trim().isEmpty()) {
            return new String[0];
        }

        java.util.List<String> tokens = new java.util.ArrayList<>();
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
     * 简单的执行上下文实现
     */
    private static class SimpleExecuteContext implements ExecuteContext {
        private final String root;
        private final String command;

        SimpleExecuteContext(String root, String command) {
            this.root = root;
            this.command = command;
        }

        @Override
        public String getRoot() {
            return root;
        }

        @Override
        public String getCommand() {
            return command;
        }
    }
}
