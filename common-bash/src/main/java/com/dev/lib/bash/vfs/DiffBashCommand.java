package com.dev.lib.bash.vfs;

import com.dev.lib.bash.ExecuteContext;
import com.dev.lib.storage.Vfs;

/**
 * diff 命令 - 文件对比
 */
public class DiffBashCommand extends VfsCommand<String> {

    @Override
    public String execute(ExecuteContext ctx) {
        String[] args = parseArgs(ctx.getCommand());
        ParsedArgs parsed = parseArgs(args);

        if (parsed.positionalCount() < 2) {
            throw new IllegalArgumentException("diff: missing file operand (need 2 files)");
        }

        String pathA = parsed.getString(0);
        String pathB = parsed.getString(1);

        var vfsCtx = toVfsContext(ctx);
        return Vfs.path(vfsCtx, pathA).diff(pathB);
    }
}
