package com.dev.lib.storage.domain.command.vfs;

import com.dev.lib.bash.ExecuteContext;
import com.dev.lib.storage.domain.model.VfsContext;
import com.dev.lib.storage.domain.model.VfsNode;
import com.dev.lib.storage.domain.service.VirtualFileSystem;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * grep 命令
 * 支持: -r 递归, -l 只输出文件名, -n 显示行号, -i 忽略大小写
 */
public class GrepCommand extends VfsCommandBase {

    public GrepCommand(VirtualFileSystem vfs) {
        super(vfs);
    }

    @Override
    public Object execute(ExecuteContext ctx) {
        String[] args = parseArgs(ctx.getCommand());
        ParsedArgs parsed = parseArgs(args);

        boolean recursive = parsed.hasFlag("r");
        boolean filesOnly = parsed.hasFlag("l");
        boolean showLineNum = parsed.hasFlag("n");
        boolean ignoreCase = parsed.hasFlag("i");

        if (parsed.positionalCount() < 2) {
            throw new IllegalArgumentException("grep: missing operand");
        }

        String pattern = parsed.getString(0);
        String path = parsed.getString(1);

        VfsContext vfsCtx = toVfsContext(ctx);

        // -l 模式：只返回文件名列表
        if (filesOnly) {
            return vfs.findByContent(vfsCtx, path, pattern, recursive);
        }

        // 默认模式：返回匹配行
        List<String> results = new ArrayList<>();
        List<VfsNode> matchedFiles = vfs.findByContent(vfsCtx, path, pattern, recursive);

        Pattern regex = ignoreCase
                ? Pattern.compile(pattern, Pattern.CASE_INSENSITIVE)
                : Pattern.compile(pattern);

        for (VfsNode node : matchedFiles) {
            grepFile(vfsCtx, node.getPath(), regex, showLineNum, matchedFiles.size() > 1, results);
        }

        return results.isEmpty() ? "" : String.join("\n", results) + "\n";
    }

    private void grepFile(VfsContext ctx, String path, Pattern pattern,
                          boolean showLineNum, boolean showFileName, List<String> results) {
        try (InputStream is = vfs.openFile(ctx, path);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

            String line;
            int lineNum = 0;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                if (pattern.matcher(line).find()) {
                    StringBuilder sb = new StringBuilder();
                    if (showFileName) {
                        sb.append(path).append(":");
                    }
                    if (showLineNum) {
                        sb.append(lineNum).append(":");
                    }
                    sb.append(line);
                    results.add(sb.toString());
                }
            }
        } catch (Exception ignored) {
        }
    }
}
