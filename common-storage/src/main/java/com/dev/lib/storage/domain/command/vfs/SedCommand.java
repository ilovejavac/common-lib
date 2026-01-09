package com.dev.lib.storage.domain.command.vfs;

import com.dev.lib.bash.ExecuteContext;
import com.dev.lib.storage.domain.service.VirtualFileSystem;

import java.util.ArrayList;
import java.util.List;

/**
 * sed 命令
 * 支持：
 * - 替换：sed 's/old/new/g' file 或 sed '5,10s/old/new/g' file
 * - 删除行：sed '5d' file 或 sed '5,10d' file
 * - 插入行：sed '5i\content' file
 * - 追加行：sed '5a\content' file
 * - 替换行：sed '5c\content' file
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
        String filePath = args[args.length - 1];

        // 读取文件内容
        String content = vfs.readFile(toVfsContext(ctx), filePath);
        if (content == null) {
            return null; // 文件不存在或读取失败
        }

        String[] lines = content.split("\n", -1);
        List<String> result = processExpression(expression, lines);

        // 写回文件
        vfs.writeFile(toVfsContext(ctx), filePath, String.join("\n", result));
        return null;
    }

    private List<String> processExpression(String expression, String[] lines) {
        // 解析行号范围和命令
        LineRange range = parseLineRange(expression);
        String cmd = extractCommand(expression);

        if (cmd.startsWith("s/")) {
            return processSubstitute(expression, lines, range);
        } else if (cmd.endsWith("d")) {
            return processDelete(expression, lines, range);
        } else if (cmd.contains("i\\")) {
            return processInsert(expression, lines, true);
        } else if (cmd.contains("a\\")) {
            return processInsert(expression, lines, false);
        } else if (cmd.contains("c\\")) {
            return processChangeLine(expression, lines);
        }

        // 默认返回原内容
        List<String> result = new ArrayList<>();
        for (String line : lines) {
            result.add(line);
        }
        return result;
    }

    /**
     * 解析行号范围，如 "5,10" 或 "5"
     */
    private LineRange parseLineRange(String expression) {
        // 查找命令起始位置
        int cmdStart = findCommandStart(expression);
        if (cmdStart == 0) {
            return new LineRange(1, -1); // 全部行
        }

        String rangeStr = expression.substring(0, cmdStart);
        int commaIdx = rangeStr.indexOf(',');
        if (commaIdx >= 0) {
            int start = Integer.parseInt(rangeStr.substring(0, commaIdx).trim());
            String endStr = rangeStr.substring(commaIdx + 1).trim();
            int end = endStr.equals("$") ? -1 : Integer.parseInt(endStr);
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
                // 检查是否是命令（s 后面必须是 /，其他后面必须是 \ 或是单独字符）
                if (c == 's' && i + 1 < expression.length() && expression.charAt(i + 1) == '/') {
                    return i;
                } else if (c == 'd' && i == expression.length() - 1) {
                    return i;
                } else if ((c == 'i' || c == 'a' || c == 'c') && i + 1 < expression.length() && expression.charAt(i + 1) == '\\') {
                    return i;
                } else if (c == 'd' && (i == 0 || expression.charAt(i - 1) == ',')) {
                    // 单独的 d 命令
                    continue;
                } else if (c == 'd') {
                    return i;
                }
            }
        }
        return 0;
    }

    private List<String> processSubstitute(String expression, String[] lines, LineRange range) {
        String cmd = extractCommand(expression);
        int lastSlash = cmd.lastIndexOf('/');
        if (lastSlash < 2) {
            return ListOf(lines);
        }

        String pattern = cmd.substring(2, lastSlash);
        String replacement = cmd.substring(lastSlash + 1);
        boolean global = (lastSlash + 1 < cmd.length()) &&
                         (cmd.charAt(lastSlash + 1) == 'g') &&
                         (cmd.length() == lastSlash + 2 ||
                          cmd.charAt(lastSlash + 2) == 'I');

        List<String> result = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            // 只在指定行号范围内替换
            if (range.isInRange(i + 1, lines.length)) {
                String newLine = global ? line.replaceAll(pattern, replacement) : line.replaceFirst(pattern, replacement);
                result.add(newLine);
            } else {
                result.add(line);
            }
        }
        return result;
    }

    private List<String> processDelete(String expression, String[] lines, LineRange range) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            if (!range.isInRange(i + 1, lines.length)) {
                result.add(lines[i]);
            }
        }
        return result;
    }

    private List<String> processInsert(String expression, String[] lines, boolean before) {
        String cmd = extractCommand(expression);
        int idx = before ? cmd.indexOf("i\\") : cmd.indexOf("a\\");
        String lineNumStr = cmd.substring(0, idx);
        int targetLine = lineNumStr.isEmpty() ? -1 : Integer.parseInt(lineNumStr);
        String newContent = cmd.substring(idx + 2);

        List<String> result = new ArrayList<>();
        boolean inserted = false;

        for (int i = 0; i < lines.length; i++) {
            if (targetLine == -1 || i + 1 == targetLine) {
                if (before) {
                    result.add(newContent);
                    inserted = true;
                }
            }
            result.add(lines[i]);
            if (targetLine == -1 || i + 1 == targetLine) {
                if (!before) {
                    result.add(newContent);
                    inserted = true;
                }
            }
        }

        // 如果目标行号超出文件行数，追加到末尾
        if (!inserted && (targetLine == -1 || targetLine > lines.length)) {
            result.add(newContent);
        }

        return result;
    }

    private List<String> processChangeLine(String expression, String[] lines) {
        String cmd = extractCommand(expression);
        int idx = cmd.indexOf("c\\");
        String lineNumStr = cmd.substring(0, idx);
        int targetLine = Integer.parseInt(lineNumStr);
        String newContent = cmd.substring(idx + 2);

        List<String> result = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            if (i + 1 == targetLine) {
                result.add(newContent);
            } else {
                result.add(lines[i]);
            }
        }
        return result;
    }

    private List<String> ListOf(String[] arr) {
        List<String> list = new ArrayList<>();
        for (String s : arr) {
            list.add(s);
        }
        return list;
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
            if (start == 1 && end == -1) return true; // 全部行
            if (lineNum < start) return false;
            if (end == -1) return lineNum >= start;
            if (end == -2 && lineNum == totalLines) return true; // $ 表示最后一行
            return lineNum <= end;
        }
    }
}
