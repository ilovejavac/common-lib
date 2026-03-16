package com.dev.lib.bash.vfs;

import com.dev.lib.bash.ExecuteContext;
import com.dev.lib.storage.Vfs;
import com.dev.lib.storage.domain.model.VfsContext;
import com.dev.lib.storage.domain.model.VfsNode;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * grep 命令 - 文本搜索
 * 支持: -r/-R 递归, -l 只输出文件名, -n 显示行号, -i 忽略大小写, -F 固定字符串
 */
public class GrepCommand extends VfsCommand<String> {

    @Override
    public String execute(ExecuteContext ctx) {
        String[] args = parseArgs(ctx.getCommand());
        ParsedArgs parsed = parseArgs(args);

        boolean recursive = parsed.hasFlag("r") || parsed.hasFlag("R");
        boolean filesOnly = parsed.hasFlag("l");
        boolean showLineNum = parsed.hasFlag("n");
        boolean ignoreCase = parsed.hasFlag("i");
        boolean fixedString = parsed.hasFlag("F");

        if (parsed.positionalCount() < 2) {
            throw new IllegalArgumentException("grep: missing operand");
        }

        String patternText = parsed.getString(0);
        VfsContext vfsCtx = toVfsContext(ctx);

        // 收集目标文件
        List<String> files = collectTargetFiles(vfsCtx, parsed, recursive);
        boolean showFileName = files.size() > 1;

        // 编译正则（非固定字符串模式）
        Pattern regex = null;
        if (!fixedString) {
            regex = ignoreCase
                    ? Pattern.compile(patternText, Pattern.CASE_INSENSITIVE)
                    : Pattern.compile(patternText);
        }

        // 逐文件搜索
        List<String> output = new ArrayList<>();
        Set<String> matchedFiles = new LinkedHashSet<>();

        for (String file : files) {
            String content;
            try {
                content = Vfs.path(vfsCtx, file).cat().executeAsString();
            } catch (Exception e) {
                throw new RuntimeException("grep: failed to read file: " + file, e);
            }

            String[] lines = content.split("\n", -1);
            for (int lineNum = 1; lineNum <= lines.length; lineNum++) {
                String line = lines[lineNum - 1];
                if (!isMatch(line, patternText, regex, ignoreCase, fixedString)) {
                    continue;
                }

                matchedFiles.add(file);
                if (filesOnly) {
                    break;
                }

                StringBuilder sb = new StringBuilder();
                if (showFileName) sb.append(file).append(":");
                if (showLineNum) sb.append(lineNum).append(":");
                sb.append(line);
                output.add(sb.toString());
            }
        }

        if (filesOnly) {
            return matchedFiles.isEmpty() ? "" : String.join("\n", matchedFiles) + "\n";
        }
        return output.isEmpty() ? "" : String.join("\n", output) + "\n";
    }

    private List<String> collectTargetFiles(VfsContext ctx, ParsedArgs parsed, boolean recursive) {
        List<String> files = new ArrayList<>();
        for (int i = 1; i < parsed.positionalCount(); i++) {
            String operand = parsed.getString(i);
            if (Vfs.path(ctx, operand).isDirectory()) {
                if (!recursive) {
                    throw new IllegalArgumentException("grep: " + operand + ": Is a directory");
                }
                List<VfsNode> nodes = Vfs.path(ctx, operand).find("*");
                for (VfsNode node : nodes) {
                    if (!Boolean.TRUE.equals(node.getIsDirectory())) {
                        files.add(node.getPath());
                    }
                }
            } else {
                files.add(operand);
            }
        }
        return files;
    }

    private boolean isMatch(String line, String patternText, Pattern pattern,
                            boolean ignoreCase, boolean fixedString) {
        if (fixedString) {
            return ignoreCase
                    ? line.toLowerCase().contains(patternText.toLowerCase())
                    : line.contains(patternText);
        }
        return pattern != null && pattern.matcher(line).find();
    }
}
