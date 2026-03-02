package com.dev.lib.storage.domain.command.vfs;

import com.dev.lib.bash.ExecuteContext;
import com.dev.lib.storage.Vfs;

/**
 * mkdir 命令
 */
public class MkdirCommand extends VfsCommandBase {

    public MkdirCommand() {

        super();
    }

    @Override
    public Object execute(ExecuteContext ctx) {

        String[]   args          = parseArgs(ctx.getCommand());
        ParsedArgs parsed        = parseArgs(args);
        boolean    createParents = parsed.hasFlag("r");

        if (parsed.positionalCount() == 0) {
            throw new IllegalArgumentException("mkdir: missing operand");
        }

        for (int i = 0; i < parsed.positionalCount(); i++) {
            String path = parsed.getString(i);
            Vfs.createDirectory(toVfsContext(ctx), path, createParents);
        }

        return null;
    }

}
