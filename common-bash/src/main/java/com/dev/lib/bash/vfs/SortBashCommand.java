package com.dev.lib.bash.vfs;

import com.dev.lib.bash.ExecuteContext;
import com.dev.lib.storage.Vfs;

import java.util.Set;

/**
 * sort 命令 - 排序
 * 支持: -r 逆序
 */
public class SortBashCommand extends VfsCommand<String> {

    @Override
    public String execute(ExecuteContext ctx) {
        String[] args = parseArgs(ctx.getCommand());
        ParsedArgs parsed = parseArgs(args, Set.of("r"), Set.of());

        if (parsed.positionalCount() == 0) {
            throw new IllegalArgumentException("sort: missing file operand");
        }

        String path = parsed.getString(0);
        boolean reverse = parsed.hasFlag("r");
        var vfsCtx = toVfsContext(ctx);

        try {
            return Vfs.path(vfsCtx, path)
                    .cat()
                    .sort(reverse)
                    .executeAsString();
        } catch (Exception e) {
            throw new RuntimeException("sort: failed to process " + path, e);
        }
    }
}
