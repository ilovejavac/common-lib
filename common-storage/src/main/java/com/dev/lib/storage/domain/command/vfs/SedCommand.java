package com.dev.lib.storage.domain.command.vfs;

import com.dev.lib.bash.ExecuteContext;
import com.dev.lib.storage.domain.service.VirtualFileSystem;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * sed 命令
 * 支持：
 * - 替换：sed 's/old/new/g' file
 * - 删除行：sed '5d' file
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
        List<String> result = new ArrayList<>();

        if (expression.startsWith("s/")) {
            // 替换命令：s/old/new/g
            return processSubstitute(expression, lines);
        } else if (expression.endsWith("d")) {
            // 删除行：5d
            return processDelete(expression, lines);
        } else if (expression.contains("i\\")) {
            // 插入行：5i\content
            return processInsert(expression, lines, true);
        } else if (expression.contains("a\\")) {
            // 追加行：5a\content
            return processInsert(expression, lines, false);
        } else if (expression.contains("c\\")) {
            // 替换行：5c\content
            return processChangeLine(expression, lines);
        }

        // 默认返回原内容
        for (String line : lines) {
            result.add(line);
        }
        return result;
    }

    private List<String> processSubstitute(String expression, String[] lines) {
        // 解析 s/old/new/g
        int lastSlash = expression.lastIndexOf('/');
        if (lastSlash < 2) {
            return ListOf(lines);
        }

        String pattern = expression.substring(2, lastSlash);
        String replacement = expression.substring(lastSlash + 1);
        boolean global = expression.endsWith("g");

        List<String> result = new ArrayList<>();
        for (String line : lines) {
            String newLine;
            if (global) {
                newLine = line.replaceAll(pattern, replacement);
            } else {
                newLine = line.replaceFirst(pattern, replacement);
            }
            result.add(newLine);
        }
        return result;
    }

    private List<String> processDelete(String expression, String[] lines) {
        // 解析 5d
        String lineNumStr = expression.substring(0, expression.length() - 1);
        int targetLine = Integer.parseInt(lineNumStr);

        List<String> result = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            if (i + 1 != targetLine) {
                result.add(lines[i]);
            }
        }
        return result;
    }

    private List<String> processInsert(String expression, String[] lines, boolean before) {
        // 解析 5i\content 或 5a\content
        int idx = before ? expression.indexOf("i\\") : expression.indexOf("a\\");
        String lineNumStr = expression.substring(0, idx);
        int targetLine = Integer.parseInt(lineNumStr);
        String newContent = expression.substring(idx + 2);

        List<String> result = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            if (i + 1 == targetLine && before) {
                result.add(newContent);
            }
            result.add(lines[i]);
            if (i + 1 == targetLine && !before) {
                result.add(newContent);
            }
        }
        return result;
    }

    private List<String> processChangeLine(String expression, String[] lines) {
        // 解析 5c\content
        int idx = expression.indexOf("c\\");
        String lineNumStr = expression.substring(0, idx);
        int targetLine = Integer.parseInt(lineNumStr);
        String newContent = expression.substring(idx + 2);

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
}
