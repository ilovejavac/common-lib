package com.dev.lib.bash.vfs;

import com.dev.lib.bash.ExecuteContext;
import com.dev.lib.storage.Vfs;

/**
 * mkdir 命令
 */
public class MkdirCommand extends VfsCommand<Void> {

    @Override
    public Void execute(ExecuteContext ctx) {

        String[]   args          = parseArgs(ctx.getCommand());
        ParsedArgs parsed        = parseArgs(args);
        boolean    createParents = parsed.hasFlag("p") || parsed.hasFlag("r");

        if (parsed.positionalCount() == 0) {
            throw new IllegalArgumentException("mkdir: missing operand");
        }

        Vfs.ContextBuilder root = Vfs.context(toVfsContext(ctx));

        for (int i = 0; i < parsed.positionalCount(); i++) {
            String path = parsed.getString(i);
            root.mkdir(path, createParents);
        }

        return null;
    }

}
