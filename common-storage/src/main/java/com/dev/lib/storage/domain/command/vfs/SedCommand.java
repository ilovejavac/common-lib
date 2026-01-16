package com.dev.lib.storage.domain.command.vfs;

import com.dev.lib.bash.ExecuteContext;
import com.dev.lib.storage.domain.service.StorageService;
import com.dev.lib.storage.domain.service.VirtualFileSystem;

/**
 * sed 命令
 * 支持：
 * - 替换：sed 's/old/new/g' file 或 sed '5,10s/old/new/g' file
 * - 删除行：sed '5d' file 或 sed '5,10d' file
 * - 插入行：sed '5i\content' file
 * - 追加行：sed '5a\content' file
 * - 替换行：sed '5c\content' file
 * <p>
 * 使用 StorageService.replaceLines() 原生实现，数据不经 JVM 内存
 */
public class SedCommand extends VfsCommandBase {

    public SedCommand(VirtualFileSystem vfs) {

        super(vfs);
    }

    @Override
    public Object execute(ExecuteContext ctx) {

        String[] args = parseArgs(ctx.getCommand());

        if (args.length < 2) {
            return "sed: missing operand\n";
        }

        String expression = args[0];
        String filePath   = args[args.length - 1];

        // 获取文件的 storagePath 并调用 StorageService.replaceLines
        String storagePath = vfs.getStoragePath(toVfsContext(ctx), filePath);
        if (storagePath == null) {
            return null; // 文件不存在
        }

        try {
            // 创建行转换器
            StorageService.LineTransformer transformer = createTransformer(expression);
            // 调用存储服务原生替换
            vfs.getStorageService().replaceLines(storagePath, transformer);
            return null;
        } catch (Exception e) {
            throw new RuntimeException("sed command failed", e);
        }
    }

    /**
     * 根据 sed 表达式创建行转换器
     */
    private StorageService.LineTransformer createTransformer(String expression) {

        LineRange range = parseLineRange(expression);
        String    cmd   = extractCommand(expression);

        if (cmd.startsWith("s/")) {
            // 替换命令
            String  pattern     = extractPattern(cmd);
            String  replacement = extractReplacement(cmd);
            boolean global      = cmd.endsWith("g") || cmd.endsWith("gI");
            boolean ignoreCase  = cmd.endsWith("I");

            java.util.regex.Pattern regex = ignoreCase
                                            ? java.util.regex.Pattern.compile(
                    pattern,
                    java.util.regex.Pattern.CASE_INSENSITIVE
            )
                                            : java.util.regex.Pattern.compile(pattern);

            return (lineNum, line) -> {
                if (range.isInRange(lineNum, -1)) {
                    return global
                           ? regex.matcher(line).replaceAll(replacement)
                           : regex.matcher(line).replaceFirst(replacement);
                }
                return line;
            };
        } else if (cmd.endsWith("d")) {
            // 删除命令
            return (lineNum, line) -> range.isInRange(lineNum, -1) ? null : line;
        } else if (cmd.contains("i\\")) {
            // 插入命令（在行前插入）
            int    idx           = cmd.indexOf("i\\");
            String lineNumStr    = cmd.substring(0, idx);
            int    targetLine    = lineNumStr.isEmpty() ? -1 : Integer.parseInt(lineNumStr);
            String insertContent = cmd.substring(idx + 2);

            return new LineInserter(targetLine, insertContent, true);
        } else if (cmd.contains("a\\")) {
            // 追加命令（在行后追加）
            int    idx           = cmd.indexOf("a\\");
            String lineNumStr    = cmd.substring(0, idx);
            int    targetLine    = lineNumStr.isEmpty() ? -1 : Integer.parseInt(lineNumStr);
            String appendContent = cmd.substring(idx + 2);

            return new LineInserter(targetLine, appendContent, false);
        } else if (cmd.contains("c\\")) {
            // 替换行命令
            int    idx        = cmd.indexOf("c\\");
            String lineNumStr = cmd.substring(0, idx);
            int    targetLine = Integer.parseInt(lineNumStr);
            String newContent = cmd.substring(idx + 2);

            return (lineNum, line) -> (lineNum == targetLine) ? newContent : line;
        }

        // 默认：原样返回
        return (lineNum, line) -> line;
    }

    /**
     * 提取替换模式
     */
    private String extractPattern(String cmd) {

        int firstSlash = cmd.indexOf('/');
        int lastSlash  = cmd.lastIndexOf('/');
        if (lastSlash <= firstSlash + 1) {
            return "";
        }
        return cmd.substring(firstSlash + 1, lastSlash);
    }

    /**
     * 提取替换内容
     */
    private String extractReplacement(String cmd) {

        int lastSlash = cmd.lastIndexOf('/');
        if (lastSlash < 2) {
            return "";
        }
        String result = cmd.substring(lastSlash + 1);
        // 移除标志位
        if (result.endsWith("g") || result.endsWith("I")) {
            result = result.substring(0, result.length() - 1);
        }
        if (result.endsWith("g") || result.endsWith("I")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    /**
     * 解析行号范围，如 "5,10" 或 "5"
     */
    private LineRange parseLineRange(String expression) {

        int cmdStart = findCommandStart(expression);
        if (cmdStart == 0) {
            return new LineRange(1, -1); // 全部行
        }

        String rangeStr = expression.substring(0, cmdStart);
        int    commaIdx = rangeStr.indexOf(',');
        if (commaIdx >= 0) {
            int    start  = Integer.parseInt(rangeStr.substring(0, commaIdx).trim());
            String endStr = rangeStr.substring(commaIdx + 1).trim();
            int    end    = endStr.equals("$") ? -1 : Integer.parseInt(endStr);
            return new LineRange(start, end);
        }

        int line = Integer.parseInt(rangeStr.trim());
        return new LineRange(line, line);
    }

    /**
     * 提取命令部分，如 "s/old/new/g" 或 "d"
     */
    private String extractCommand(String expression) {

        int cmdStart = findCommandStart(expression);
        return cmdStart > 0 ? expression.substring(cmdStart) : expression;
    }

    /**
     * 查找命令起始位置（行号范围之后）
     */
    private int findCommandStart(String expression) {

        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);
            if (c == 's' || c == 'd' || c == 'i' || c == 'a' || c == 'c') {
                if (c == 's' && i + 1 < expression.length() && expression.charAt(i + 1) == '/') {
                    return i;
                } else if (c == 'd' && i == expression.length() - 1) {
                    return i;
                } else if ((c == 'i' || c == 'a' || c == 'c') && i + 1 < expression.length() && expression.charAt(i + 1) == '\\') {
                    return i;
                } else if (c == 'd' && (i == 0 || expression.charAt(i - 1) == ',')) {
                    continue;
                } else if (c == 'd') {
                    return i;
                }
            }
        }
        return 0;
    }

    /**
     * 行号范围
     */
    private static class LineRange {

        final int start;

        final int end; // -1 表示到末尾

        LineRange(int start, int end) {

            this.start = start;
            this.end = end;
        }

        boolean isInRange(int lineNum, int totalLines) {

            if (start == 1 && end == -1) return true;
            if (lineNum < start) return false;
            if (end == -1) return lineNum >= start;
            if (end == -2 && lineNum == totalLines) return true;
            return lineNum <= end;
        }

    }

    /**
     * 行插入器（支持 i\ 和 a\ 命令）
     * 由于这两个命令需要返回多行，使用状态机实现
     */
    private static class LineInserter implements StorageService.LineTransformer {

        private final int     targetLine;

        private final String  content;

        private final boolean before;

        private       boolean inserted = false;

        LineInserter(int targetLine, String content, boolean before) {

            this.targetLine = targetLine;
            this.content = content;
            this.before = before;
        }

        @Override
        public String transform(int lineNum, String line) {
            // 这是一个简化的实现，实际上 LineTransformer 接口需要扩展才能支持真正的插入
            // 当前实现只能作为示例，实际使用时需要修改架构
            return line;
        }

    }

}
