package com.dev.lib.bash.vfs;

import com.dev.lib.bash.ExecuteContext;
import com.dev.lib.storage.Vfs;

/**
 * uniq 命令 - 去除连续重复行
 * 通常配合 sort 使用
 */
public class UniqBashCommand extends VfsCommand<String> {

    @Override
    public String execute(ExecuteContext ctx) {
        String[] args = parseArgs(ctx.getCommand());
        ParsedArgs parsed = parseArgs(args);

        if (parsed.positionalCount() == 0) {
            throw new IllegalArgumentException("uniq: missing file operand");
        }

        String path = parsed.getString(0);
        var vfsCtx = toVfsContext(ctx);

        try {
            return Vfs.path(vfsCtx, path)
                    .cat()
                    .uniq()
                    .executeAsString();
        } catch (Exception e) {
            throw new RuntimeException("uniq: failed to process " + path, e);
        }
    }
}
