package com.dev.lib.bash.vfs;

import com.dev.lib.bash.ExecuteContext;
import com.dev.lib.storage.Vfs;
import com.dev.lib.storage.domain.model.VfsContext;
import com.dev.lib.storage.domain.model.VfsNode;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * grep 命令
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

        String pattern = parsed.getString(0);
        VfsContext vfsCtx = toVfsContext(ctx);

        List<String> operands = new ArrayList<>();
        for (int i = 1; i < parsed.positionalCount(); i++) {
            operands.add(parsed.getString(i));
        }

        List<String> files = collectTargetFiles(vfsCtx, operands, recursive);
        boolean showFileName = files.size() > 1;

        List<String> output = new ArrayList<>();
        Set<String> matchedFileSet = new LinkedHashSet<>();

        Pattern regex = null;
        if (!fixedString) {
            regex = ignoreCase
                    ? Pattern.compile(pattern, Pattern.CASE_INSENSITIVE)
                    : Pattern.compile(pattern);
        }

        for (String file : files) {
            boolean matched = grepFile(
                    vfsCtx, file, pattern, regex,
                    showLineNum, showFileName, filesOnly,
                    ignoreCase, fixedString, output
            );
            if (matched) {
                matchedFileSet.add(file);
            }
        }

        if (filesOnly) {
            if (matchedFileSet.isEmpty()) {
                return "";
            }
            return String.join("\n", matchedFileSet) + "\n";
        }

        return output.isEmpty() ? "" : String.join("\n", output) + "\n";
    }

    private List<String> collectTargetFiles(VfsContext ctx, List<String> operands, boolean recursive) {
        List<String> files = new ArrayList<>();
        for (String operand : operands) {
            if (Vfs.context(ctx).file(operand).isDirectory()) {
                if (!recursive) {
                    throw new IllegalArgumentException("grep: " + operand + ": Is a directory");
                }
                List<VfsNode> nodes = Vfs.context(ctx).findByName(operand, "*", true);
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

    private boolean grepFile(
            VfsContext ctx, String path, String patternText, Pattern pattern,
            boolean showLineNum, boolean showFileName, boolean filesOnly,
            boolean ignoreCase, boolean fixedString, List<String> output
    ) {
        try (InputStream is = Vfs.context(ctx).file(path).open();
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

            String line;
            int lineNum = 0;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                if (!isMatch(line, patternText, pattern, ignoreCase, fixedString)) {
                    continue;
                }

                if (filesOnly) {
                    return true;
                }

                StringBuilder sb = new StringBuilder();
                if (showFileName) {
                    sb.append(path).append(":");
                }
                if (showLineNum) {
                    sb.append(lineNum).append(":");
                }
                sb.append(line);
                output.add(sb.toString());
            }
            return false;
        } catch (Exception e) {
            throw new RuntimeException("grep: failed to read file: " + path, e);
        }
    }

    private boolean isMatch(
            String line, String patternText, Pattern pattern,
            boolean ignoreCase, boolean fixedString
    ) {
        if (fixedString) {
            if (ignoreCase) {
                return line.toLowerCase().contains(patternText.toLowerCase());
            }
            return line.contains(patternText);
        }
        return pattern != null && pattern.matcher(line).find();
    }
}
