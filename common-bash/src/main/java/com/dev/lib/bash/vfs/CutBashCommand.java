package com.dev.lib.bash.vfs;

import com.dev.lib.bash.ExecuteContext;
import com.dev.lib.storage.Vfs;

import java.util.Set;

/**
 * cut 命令 - 按分隔符提取列
 * 支持: cut -d',' -f1,3 file
 */
public class CutBashCommand extends VfsCommand<String> {

    @Override
    public String execute(ExecuteContext ctx) {
        String[] args = parseArgs(ctx.getCommand());
        ParsedArgs parsed = parseArgs(args, Set.of("d", "f"), Set.of());

        String delimiter = parsed.options.get("d") instanceof String s ? s : null;
        String fieldsStr = parsed.options.get("f") instanceof String s ? s : null;

        if (delimiter == null) delimiter = "\t";
        if (fieldsStr == null) {
            throw new IllegalArgumentException("cut: you must specify a list of fields");
        }

        if (parsed.positionalCount() == 0) {
            throw new IllegalArgumentException("cut: missing file operand");
        }

        // 解析字段列表: "1,3,5" → int[]
        int[] fields = java.util.Arrays.stream(fieldsStr.split(","))
                .mapToInt(s -> {
                    try {
                        return Integer.parseInt(s.trim());
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("cut: invalid field: " + s);
                    }
                })
                .toArray();

        String path = parsed.getString(0);
        var vfsCtx = toVfsContext(ctx);

        try {
            return Vfs.path(vfsCtx, path)
                    .cat()
                    .cut(delimiter, fields)
                    .executeAsString();
        } catch (Exception e) {
            throw new RuntimeException("cut: failed to process " + path, e);
        }
    }
}
