package com.dev.lib.bash.vfs;

import com.dev.lib.bash.ExecuteContext;
import com.dev.lib.storage.Vfs;

import java.util.Set;

/**
 * sed 命令 - 流式替换
 * 支持: sed 's/pattern/replacement/g' file
 */
public class SedBashCommand extends VfsCommand<String> {

    @Override
    public String execute(ExecuteContext ctx) {
        String[] args = parseArgs(ctx.getCommand());
        ParsedArgs parsed = parseArgs(args, Set.of("i"), Set.of());

        if (parsed.positionalCount() < 2) {
            throw new IllegalArgumentException("sed: usage: sed 's/pattern/replacement/' file");
        }

        String expression = parsed.getString(0);
        String path = parsed.getString(1);
        boolean inPlace = parsed.hasFlag("i");

        // 解析 s/pattern/replacement/ 格式
        if (!expression.startsWith("s/") && !expression.startsWith("s|")) {
            throw new IllegalArgumentException("sed: only s/pattern/replacement/ syntax is supported");
        }

        char delimiter = expression.charAt(1);
        String[] parts = expression.substring(2).split(java.util.regex.Pattern.quote(String.valueOf(delimiter)), -1);
        if (parts.length < 2) {
            throw new IllegalArgumentException("sed: invalid expression: " + expression);
        }

        String pattern = parts[0];
        String replacement = parts[1];

        var vfsCtx = toVfsContext(ctx);

        try {
            if (inPlace) {
                // sed -i: 替换并写回原文件
                String result = Vfs.path(vfsCtx, path)
                        .cat()
                        .sed(pattern, replacement)
                        .executeAsString();
                Vfs.path(vfsCtx, path).write(result);
                return "";
            } else {
                return Vfs.path(vfsCtx, path)
                        .cat()
                        .sed(pattern, replacement)
                        .executeAsString();
            }
        } catch (Exception e) {
            throw new RuntimeException("sed: failed to process " + path, e);
        }
    }
}
