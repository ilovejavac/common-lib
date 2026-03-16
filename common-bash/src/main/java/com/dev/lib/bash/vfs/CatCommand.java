package com.dev.lib.bash.vfs;

import com.dev.lib.bash.ExecuteContext;
import com.dev.lib.storage.Vfs;

import java.util.Set;

/**
 * cat 命令 - 显示文件内容
 * 支持: -n 显示行号, -s startLine, -c lineCount
 */
public class CatCommand extends VfsCommand<String> {

    @Override
    public String execute(ExecuteContext ctx) {
        String[] args = parseArgs(ctx.getCommand());
        ParsedArgs parsed = parseArgs(args, Set.of("n", "s", "c"), Set.of());
        boolean showLineNumbers = parsed.hasFlag("n");
        int startLine = parsed.getInt("s", 1);
        int lineCount = parsed.getInt("c", -1);

        if (startLine < 1) {
            throw new IllegalArgumentException("cat: invalid start line: " + startLine);
        }
        if (lineCount < -1) {
            throw new IllegalArgumentException("cat: invalid line count: " + lineCount);
        }
        if (parsed.positionalCount() == 0) {
            throw new IllegalArgumentException("cat: missing file operand");
        }

        var vfsCtx = toVfsContext(ctx);
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < parsed.positionalCount(); i++) {
            String path = parsed.getString(i);
            String content = readContent(vfsCtx, path, startLine, lineCount);

            if (showLineNumbers) {
                result.append(addLineNumbers(content, startLine));
            } else {
                result.append(content);
            }
        }

        return result.toString();
    }

    private String readContent(com.dev.lib.storage.domain.model.VfsContext ctx, String path, int startLine, int lineCount) {
        try {
            if (startLine > 1 || lineCount != -1) {
                // 有范围限制：读取全部然后截取
                String all = Vfs.path(ctx, path).cat().executeAsString();
                String[] lines = all.split("\n", -1);
                int start = Math.min(startLine - 1, lines.length);
                int end = lineCount == -1 ? lines.length : Math.min(start + lineCount, lines.length);

                StringBuilder sb = new StringBuilder();
                for (int i = start; i < end; i++) {
                    sb.append(lines[i]).append("\n");
                }
                return sb.toString();
            }
            return Vfs.path(ctx, path).cat().executeAsString();
        } catch (Exception e) {
            throw new RuntimeException("cat: failed to read " + path, e);
        }
    }

    private String addLineNumbers(String content, int startLineNum) {
        if (content.isEmpty()) {
            return content;
        }
        String[] lines = content.split("\n", -1);
        StringBuilder sb = new StringBuilder();
        int lineNum = startLineNum;
        for (int i = 0; i < lines.length; i++) {
            // 跳过末尾 split 产生的空行
            if (i == lines.length - 1 && lines[i].isEmpty()) {
                continue;
            }
            sb.append(String.format("%6d\t%s\n", lineNum++, lines[i]));
        }
        return sb.toString();
    }
}
