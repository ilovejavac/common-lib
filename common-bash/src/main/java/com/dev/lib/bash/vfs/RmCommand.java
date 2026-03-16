package com.dev.lib.bash.vfs;

import com.dev.lib.bash.ExecuteContext;
import com.dev.lib.storage.Vfs;

/**
 * rm 命令
 */
public class RmCommand extends VfsCommand<Void> {

    @Override
    public Void execute(ExecuteContext ctx) {
        String[] args = parseArgs(ctx.getCommand());
        ParsedArgs parsed = parseArgs(args);
        boolean recursive = parsed.hasFlag("r") || parsed.hasFlag("R");
        boolean force = parsed.hasFlag("f");

        if (parsed.positionalCount() == 0) {
            throw new IllegalArgumentException("rm: missing operand");
        }

        var vfsCtx = toVfsContext(ctx);
        for (int i = 0; i < parsed.positionalCount(); i++) {
            String path = parsed.getString(i);
            try {
                Vfs.path(vfsCtx, path).rm(recursive);
            } catch (IllegalArgumentException e) {
                if (!force || !e.getMessage().contains("not found")) {
                    throw e;
                }
            }
        }

        return null;
    }
}
