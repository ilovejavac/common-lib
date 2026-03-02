package com.dev.lib.storage.domain.command.vfs;

import com.dev.lib.bash.ExecuteContext;
import com.dev.lib.storage.Vfs;

/**
 * touch 命令
 */
public class TouchCommand extends VfsCommandBase {

    public TouchCommand() {

        super();
    }

    @Override
    public Object execute(ExecuteContext ctx) {

        String[]   args   = parseArgs(ctx.getCommand());
        ParsedArgs parsed = parseArgs(args);

        if (parsed.positionalCount() == 0) {
            throw new IllegalArgumentException("touch: missing file operand");
        }

        for (int i = 0; i < parsed.positionalCount(); i++) {
            String path = parsed.getString(i);
            Vfs.touchFile(toVfsContext(ctx), path);
        }

        return null;
    }

}
