package com.dev.lib.bash.vfs;

import com.dev.lib.bash.ExecuteContext;
import com.dev.lib.storage.Vfs;

/**
 * touch 命令
 */
public class TouchCommand extends VfsCommand<Void> {

    @Override
    public Void execute(ExecuteContext ctx) {
        String[] args = parseArgs(ctx.getCommand());
        ParsedArgs parsed = parseArgs(args);

        if (parsed.positionalCount() == 0) {
            throw new IllegalArgumentException("touch: missing file operand");
        }

        var vfsCtx = toVfsContext(ctx);
        for (int i = 0; i < parsed.positionalCount(); i++) {
            String path = parsed.getString(i);
            Vfs.path(vfsCtx, path).touch();
        }

        return null;
    }
}
